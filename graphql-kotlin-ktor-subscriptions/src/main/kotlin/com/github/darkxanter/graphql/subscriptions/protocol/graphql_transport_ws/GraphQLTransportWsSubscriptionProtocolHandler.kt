package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerError
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.darkxanter.graphql.subscriptions.ApolloSubscriptionHooks
import com.github.darkxanter.graphql.subscriptions.KtorGraphQLSubscriptionHandler
import com.github.darkxanter.graphql.subscriptions.SubscriptionProtocolHandler
import com.github.darkxanter.graphql.subscriptions.castToMapOfStringString
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLTransportWsSubscriptionOperationMessage
import io.ktor.server.application.ApplicationCall
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job
import org.slf4j.LoggerFactory
import kotlin.time.Duration

/**
 * Implementation of the `graphql-transport-ws` protocol
 *
 * https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 */
// TODO implement socket close for duplicated operations
public class GraphQLTransportWsSubscriptionProtocolHandler(
    private val contextFactory: GraphQLContextFactory<*, ApplicationCall>,
    private val subscriptionHandler: KtorGraphQLSubscriptionHandler,
    private val objectMapper: ObjectMapper,
    private val subscriptionHooks: ApolloSubscriptionHooks,
    private val pingInterval: Duration = Duration.ZERO,
) : SubscriptionProtocolHandler<GraphQLTransportWsSubscriptionOperationMessage> {
    public companion object {
        public const val badRequestCode: Short = 4400
    }

    private val sessionState = GraphQLTransportWsSubscriptionSessionState()
    private val logger = LoggerFactory.getLogger(GraphQLTransportWsSubscriptionProtocolHandler::class.java)

    @Suppress("Detekt.TooGenericExceptionCaught")
    override suspend fun handle(payload: String, session: WebSocketServerSession): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        val operationMessage = convertToMessageOrNull(payload) ?: return closeWithInvalidMessage(
            session,
            "Unknown message or message with missing fields",
        )
        logger.debug("GraphQL subscription client, session=$session operationMessage=$operationMessage")

        return try {
            when (operationMessage) {
                is GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit -> onInit(operationMessage, session)
                is GraphQLTransportWsSubscriptionOperationMessage.Subscribe -> startSubscription(operationMessage, session)
                is GraphQLTransportWsSubscriptionOperationMessage.Complete -> onComplete(operationMessage, session)
                is GraphQLTransportWsSubscriptionOperationMessage.Ping -> flowOf(GraphQLTransportWsSubscriptionOperationMessage.Pong())
                is GraphQLTransportWsSubscriptionOperationMessage.Pong -> TODO()

                is GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck,
                is GraphQLTransportWsSubscriptionOperationMessage.Next,
                is GraphQLTransportWsSubscriptionOperationMessage.Error -> {
                    closeWithInvalidMessage(session, "Error message can be sent only from server")
                }
            }
        } catch (exception: Exception) {
            onException(session, operationMessage, exception)
        }
    }

    @Suppress("Detekt.TooGenericExceptionCaught")
    private fun convertToMessageOrNull(payload: String): GraphQLTransportWsSubscriptionOperationMessage? {
        return try {
            objectMapper.readValue(payload)
        } catch (exception: Exception) {
            logger.error("Error parsing the subscription message", exception)
            null
        }
    }

    /**
     * If the keep alive configuration is set, send a message back to client at every interval until the session is terminated.
     * Otherwise, just return empty flow to append to the acknowledgment message.
     */
    private fun getPingFlow(session: WebSocketServerSession): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        if (pingInterval.isPositive()) {
            return flow {
                while (true) {
                    emit(GraphQLTransportWsSubscriptionOperationMessage.Ping())
                    delay(pingInterval.inWholeMilliseconds)
                }
            }.onStart {
                sessionState.saveKeepAliveSubscription(session, currentCoroutineContext().job)
            }
        }
        return emptyFlow()
    }

    @Suppress("Detekt.TooGenericExceptionCaught")
    private fun startSubscription(
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage.Subscribe,
        session: WebSocketServerSession
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        val graphQLContext = sessionState.getGraphQLContext(session)

        subscriptionHooks.onOperation(operationMessage, session, graphQLContext)

        if (sessionState.doesOperationExist(session, operationMessage)) {
            logger.info("Already subscribed to operation ${operationMessage.id} for session $session")
            return emptyFlow()
        }

        try {
            return subscriptionHandler.executeSubscription(operationMessage.payload, graphQLContext)
                .map<GraphQLResponse<*>, GraphQLTransportWsSubscriptionOperationMessage> {
                    if (it.errors?.isNotEmpty() == true) {
                        GraphQLTransportWsSubscriptionOperationMessage.Error(
                            id = operationMessage.id,
                            payload = it.errors!!,
                        )
                    } else {
                        GraphQLTransportWsSubscriptionOperationMessage.Next(
                            id = operationMessage.id,
                            payload = it
                        )
                    }
                }
                .onStart {
                    val job = currentCoroutineContext().job
                    sessionState.saveOperation(session, operationMessage, job)
                }
                .onCompletion { error ->
                    if (error == null)
                        emitAll(onComplete(operationMessage, session))
                }
        } catch (exception: Exception) {
            logger.error("Error running graphql subscription", exception)
            // Do not terminate the session, just stop the operation messages
            sessionState.stopOperation(session, operationMessage)
            return flowOf(
                GraphQLTransportWsSubscriptionOperationMessage.Error(
                    id = operationMessage.id,
                    payload = listOf(GraphQLServerError("An internal error occurred while subscribing"))
                )
            )
        }
    }

    private suspend fun onInit(
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit,
        session: WebSocketServerSession,
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        saveContext(operationMessage, session)
        val acknowledgeMessage = flowOf<GraphQLTransportWsSubscriptionOperationMessage>(GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck())
        val keepAliveFlow = getPingFlow(session)
        return acknowledgeMessage.onCompletion { if (it == null) emitAll(keepAliveFlow) }
            .catch {
                closeWithInvalidMessage(session, "Connection initialization failed")
            }
    }

    /**
     * Generate the context and save it for all future messages.
     */
    private suspend fun saveContext(
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit,
        session: WebSocketServerSession
    ) {
        val connectionParams = castToMapOfStringString(operationMessage.payload)
        val graphQLContext = contextFactory.generateContextMap(session.call)
        val onConnectContext = subscriptionHooks.onConnect(connectionParams, session, graphQLContext)
        sessionState.saveContextMap(session, onConnectContext)
    }

    /**
     * Called with the publisher has completed on its own.
     */
    private fun onComplete(
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage,
        session: WebSocketServerSession
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        subscriptionHooks.onOperationComplete(session)
        return sessionState.completeOperation(session, operationMessage)
    }

    override suspend fun onDisconnect(session: WebSocketServerSession) {
        subscriptionHooks.onDisconnect(session)
        sessionState.terminateSession(session)
    }

    private suspend fun onException(
        session: WebSocketServerSession,
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage,
        exception: Exception
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        logger.error("Error parsing the subscription message $operationMessage", exception)
        return when (operationMessage) {
            is GraphQLTransportWsSubscriptionOperationMessage.OperationMessageId -> flowOf(
                GraphQLTransportWsSubscriptionOperationMessage.Error(
                    operationMessage.id,
                    payload = listOf(
                        GraphQLServerError("Internal Error"),
                    )
                )
            )
            else -> closeWithInvalidMessage(session, "Internal Error")
        }
    }

    private suspend fun closeWithInvalidMessage(
        session: WebSocketServerSession,
        text: String
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        session.close(CloseReason(badRequestCode, text))
        return emptyFlow()
    }
}
