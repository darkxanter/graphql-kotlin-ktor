package com.github.darkxanter.graphql.subscriptions.protocol.message

import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerError
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * The WebSocket sub-protocol for this specification is: `graphql-transport-ws`.
 *
 * Messages are represented through the JSON structure and are stringified before being sent over the network. They are bidirectional, meaning both the server and the client must conform to the specified message structure.
 *
 * **All** messages contain the `type` field outlining the action this message describes. Depending on the type, the message can contain two more _optional_ fields:
 *
 * - `id` used for uniquely identifying server responses and connecting them with the client's requests
 * - `payload` holding the extra "payload" information to go with the specific message type
 *
 * Multiple operations identified with separate IDs can be active at any time and their messages can be interleaved on the connection.
 *
 * The server can close the socket (kick the client off) at any time. The close event dispatched by the server is used to describe the fatal error to the client.
 *
 * The client closes the socket and the connection by dispatching a `1000: Normal Closure close` event to the server indicating a normal closure.
 *
 * https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 */
@Suppress("unused")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface GraphQLTransportWsSubscriptionOperationMessage : SubscriptionOperationMessage {
    /**
     * Direction: **Client -> Server**
     *
     * Indicates that the client wants to establish a connection within the existing socket.
     * This connection is not the actual WebSocket communication channel,
     * but is rather a frame within it asking the server to allow future operation requests.
     *
     * The server must receive the connection initialisation message within the allowed waiting time specified in the `connectionInitWaitTimeout`
     * parameter during the server setup. If the client does not request a connection within the allowed timeout,
     * the server will close the socket with the event: `4408: Connection initialisation timeout`.
     *
     * If the server receives more than one [ConnectionInit] message at any given time,
     * the server will close the socket with the event `4429: Too many initialisation requests`.
     */
    @JsonTypeName("connection_init")
    public data class ConnectionInit(
        val payload: Map<String, Any?>? = null,
    ) : GraphQLTransportWsSubscriptionOperationMessage

    /**
     * Direction: **Server -> Client**
     *
     * Expected response to the [ConnectionInit] message from the client acknowledging a successful connection with the server.
     *
     * The server can use the optional [payload] field to transfer additional details about the connection.
     */
    @JsonTypeName("connection_ack")
    public data class ConnectionAck(
        val payload: Map<String, Any?>? = null,
    ) : GraphQLTransportWsSubscriptionOperationMessage

    /**
     * Direction: **bidirectional**
     *
     * Useful for detecting failed connections, displaying latency metrics or other types of network probing.
     *
     * A [Pong] must be sent in response from the receiving party as soon as possible.
     *
     * The [Ping] message can be sent at any time within the established socket.
     *
     * The optional [payload] field can be used to transfer additional details about the ping.
     */
    @JsonTypeName("ping")
    public data class Ping(
        val payload: Map<String, Any?>? = null,
    ) : GraphQLTransportWsSubscriptionOperationMessage

    /**
     * Direction: **bidirectional**
     *
     * The response to the [Ping] message. Must be sent as soon as the [Ping] message is received.
     *
     * The [Pong] message can be sent at any time within the established socket.
     * Furthermore, the [Pong] message may even be sent unsolicited as an unidirectional heartbeat.
     *
     * The optional [payload] field can be used to transfer additional details about the pong.
     */
    @JsonTypeName("pong")
    public data class Pong(
        val payload: Map<String, Any?>? = null,
    ) : GraphQLTransportWsSubscriptionOperationMessage


    /**
     * Direction: **Client -> Server**
     *
     * Requests an operation specified in the message [payload]. This message provides a unique ID field to connect published messages to the operation requested by this message.
     *
     * If there is already an active subscriber for an operation matching the provided ID, regardless of the operation type,
     * the server must close the socket immediately with the event `4409: Subscriber for <unique-operation-id> already exists`.
     *
     * The server needs only keep track of IDs for as long as the subscription is active. Once a client completes an operation, it is free to re-use that ID.
     *
     * Executing operations is allowed only after the server has acknowledged the connection through the [ConnectionAck] message,
     * if the connection is not acknowledged, the socket will be closed immediately with the event `4401: Unauthorized`.
     */
    @JsonTypeName("subscribe")
    public data class Subscribe(
        override val id: String,
        val payload: GraphQLRequestWS,
    ) : GraphQLTransportWsSubscriptionOperationMessage, OperationMessageId

    /**
     * Direction: **Server -> Client**
     *
     * Operation execution result(s) from the source stream created by the binding [Subscribe] message.
     * After all results have been emitted, the Complete message will follow indicating stream completion.
     */
    @JsonTypeName("next")
    public data class Next<T>(
        override val id: String,
        val payload: GraphQLResponse<T>,
    ) : GraphQLTransportWsSubscriptionOperationMessage, OperationMessageId

    /**
     * Direction: **Server -> Client**
     *
     * Operation execution error(s) in response to the [Subscribe] message.
     * This can occur before execution starts, usually due to validation errors, or during the execution of the request.
     * This message terminates the operation and no further messages will be sent.
     */
    @JsonTypeName("error")
    public data class Error(
        override val id: String,
        val payload: List<GraphQLServerError>,
    ) : GraphQLTransportWsSubscriptionOperationMessage, OperationMessageId

    /**
     * Direction: **bidirectional**
     *
     * - **Server -> Client** indicates that the requested operation execution has completed.
     * If the server dispatched the [Error] message relative to the original [Subscribe] message, no [Complete] message will be emitted.
     * - **Client -> Server** indicates that the client has stopped listening and wants to complete the subscription.
     * No further events, relevant to the original subscription, should be sent through.
     * Even if the client sent a [Complete] message for a single-result-operation before it resolved,
     * the result should not be sent through once it does.
     *
     * Note: The asynchronous nature of the full-duplex connection means that a client can send a [Complete] message to the server
     * even when messages are in-flight to the client, or when the server has itself completed the operation (via a [Error] or [Complete] message).
     * Both client and server must therefore be prepared to receive (and ignore) messages for operations that they consider already completed.
     */
    @JsonTypeName("complete")
    public data class Complete(
        override val id: String,
    ) : GraphQLTransportWsSubscriptionOperationMessage, OperationMessageId

    /**
     * Direction: **bidirectional**
     *
     * Receiving a message of a type or format which is not specified in this document will result in an __immediate__ socket closure with the event `4400: <error-message>`.
     * The `<error-message>` can be vaguely descriptive on why the received message is invalid.
     *
     * Receiving a message (other than [Subscribe]) with an ID that belongs to an operation that has been previously completed does not constitute an error.
     * It is permissible to simply ignore all unknown IDs without closing the connection.
     */
    public interface InvalidMessage


    public interface OperationMessageId {
        public val id: String
    }
}

public val GraphQLTransportWsSubscriptionOperationMessage.id: String?
    get() = when (this) {
        is GraphQLTransportWsSubscriptionOperationMessage.OperationMessageId -> id
        else -> null
    }
