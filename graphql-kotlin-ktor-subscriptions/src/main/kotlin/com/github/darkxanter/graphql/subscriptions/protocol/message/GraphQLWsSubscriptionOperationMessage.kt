package com.github.darkxanter.graphql.subscriptions.protocol.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * The `graphql-ws` protocol from Apollo Client has some special text messages to signal events.
 * Along with the HTTP WebSocket event handling we need to have some extra logic
 *
 * https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class GraphQLWsSubscriptionOperationMessage(
    val type: String,
    val id: String? = null,
    val payload: Any? = null
) : SubscriptionOperationMessage {
    public enum class ClientMessages(public val type: String) {
        GQL_CONNECTION_INIT("connection_init"),
        GQL_START("start"),
        GQL_STOP("stop"),
        GQL_CONNECTION_TERMINATE("connection_terminate")
    }

    public enum class ServerMessages(public val type: String) {
        GQL_CONNECTION_ACK("connection_ack"),
        GQL_CONNECTION_ERROR("connection_error"),
        GQL_DATA("data"),
        GQL_ERROR("error"),
        GQL_COMPLETE("complete"),
        GQL_CONNECTION_KEEP_ALIVE("ka")
    }
}
