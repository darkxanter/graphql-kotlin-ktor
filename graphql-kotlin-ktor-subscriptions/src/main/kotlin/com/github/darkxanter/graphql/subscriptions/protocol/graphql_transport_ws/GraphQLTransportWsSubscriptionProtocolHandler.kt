@file:Suppress("DEPRECATION")

package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerError
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.darkxanter.graphql.subscriptions.KtorGraphQLSubscriptionHandler
import com.github.darkxanter.graphql.subscriptions.KtorSubscriptionGraphQLContextFactory
import com.github.darkxanter.graphql.subscriptions.SubscriptionProtocolHandler
import com.github.darkxanter.graphql.subscriptions.castToMapOfStringString
import io.ktor.websocket.CloseReason
import io.ktor.websocket.WebSocketSession
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
public class GraphQLTransportWsSubscriptionProtocolHandler(
    private val contextFactory: KtorSubscriptionGraphQLContextFactory<*>,
    private val subscriptionHandler: KtorGraphQLSubscriptionHandler,
    private val objectMapper: ObjectMapper,
//    private val subscriptionHooks: ApolloSubscriptionHooks,
    private val pingInterval: Duration = Duration.ZERO,
) : SubscriptionProtocolHandler<SubscriptionOperationMessage> {
    private val sessionState = GraphQLTransportWsSubscriptionSessionState()
    private val logger = LoggerFactory.getLogger(GraphQLTransportWsSubscriptionProtocolHandler::class.java)

    @Suppress("Detekt.TooGenericExceptionCaught")
    override suspend fun handle(payload: String, session: WebSocketSession): Flow<SubscriptionOperationMessage> {
        val operationMessage = convertToMessageOrNull(payload) ?: return closeWithInvalidMessage(
            session,
            "Unknown message or message with missing fields",
        )
        logger.debug("GraphQL subscription client, session=$session operationMessage=$operationMessage")

        return try {
            when (operationMessage) {
                is SubscriptionOperationMessage.ConnectionInit -> onInit(operationMessage, session)
                is SubscriptionOperationMessage.Subscribe -> startSubscription(operationMessage, session)
                is SubscriptionOperationMessage.Complete -> onComplete(operationMessage, session)
                is SubscriptionOperationMessage.Ping -> flowOf(SubscriptionOperationMessage.Pong())
                is SubscriptionOperationMessage.Pong -> TODO()

                is SubscriptionOperationMessage.ConnectionAck,
                is SubscriptionOperationMessage.Next,
                is SubscriptionOperationMessage.Error -> {
                    closeWithInvalidMessage(session, "Error message can be sent only from server")
                }
            }
        } catch (exception: Exception) {
            onException(session, operationMessage, exception)
        }
    }

    @Suppress("Detekt.TooGenericExceptionCaught")
    private fun convertToMessageOrNull(payload: String): SubscriptionOperationMessage? {
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
    private fun getPingFlow(session: WebSocketSession): Flow<SubscriptionOperationMessage> {
        if (pingInterval.isPositive()) {
            return flow {
                while (true) {
                    emit(SubscriptionOperationMessage.Ping())
                    delay(pingInterval)
                }
            }.onStart {
                sessionState.saveKeepAliveSubscription(session, currentCoroutineContext().job)
            }
        }
        return emptyFlow()
    }

    @Suppress("Detekt.TooGenericExceptionCaught")
    private fun startSubscription(
        operationMessage: SubscriptionOperationMessage.Subscribe,
        session: WebSocketSession
    ): Flow<SubscriptionOperationMessage> {
        val context = sessionState.getContext(session)
        val graphQLContext = sessionState.getGraphQLContext(session)

//        subscriptionHooks.onOperation(operationMessage, session, context)
//        subscriptionHooks.onOperationWithContext(operationMessage, session, graphQLContext)

        if (sessionState.doesOperationExist(session, operationMessage)) {
            logger.info("Already subscribed to operation ${operationMessage.id} for session $session")
            return emptyFlow()
        }

        try {
            return subscriptionHandler.executeSubscription(operationMessage.payload, context, graphQLContext)
                .map<GraphQLResponse<*>, SubscriptionOperationMessage> {
                    if (it.errors?.isNotEmpty() == true) {
                        SubscriptionOperationMessage.Error(
                            id = operationMessage.id,
                            payload = it.errors!!,
                        )
                    } else {
                        SubscriptionOperationMessage.Next(
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
                SubscriptionOperationMessage.Error(
                    id = operationMessage.id,
                    payload = listOf(GraphQLServerError("An internal error occurred while subscribing"))
                )
            )
        }
    }

    private suspend fun onInit(
        operationMessage: SubscriptionOperationMessage.ConnectionInit,
        session: WebSocketSession,
    ): Flow<SubscriptionOperationMessage> {
        saveContext(operationMessage, session)
        val acknowledgeMessage = flowOf<SubscriptionOperationMessage>(SubscriptionOperationMessage.ConnectionAck())
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
        operationMessage: SubscriptionOperationMessage.ConnectionInit,
        session: WebSocketSession
    ) {
        val connectionParams = castToMapOfStringString(operationMessage.payload)
        val context = contextFactory.generateContext(session)
        val graphQLContext = contextFactory.generateContextMap(session)
//        val onConnectContext = subscriptionHooks.onConnect(connectionParams, session, context)
//        val onConnectGraphQLContext = subscriptionHooks.onConnectWithContext(connectionParams, session, graphQLContext)
//        sessionState.saveContext(session, onConnectContext)
//        sessionState.saveContextMap(session, onConnectGraphQLContext)
    }

    /**
     * Called with the publisher has completed on its own.
     */
    private fun onComplete(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession
    ): Flow<SubscriptionOperationMessage> {
//        subscriptionHooks.onOperationComplete(session)
        return sessionState.completeOperation(session, operationMessage)
    }

    private suspend fun onDisconnect(session: WebSocketSession): Flow<SubscriptionOperationMessage> {
//        subscriptionHooks.onDisconnect(session)
        sessionState.terminateSession(session)
        return emptyFlow()
    }

    private suspend fun onException(
        session: WebSocketSession,
        operationMessage: SubscriptionOperationMessage,
        exception: Exception
    ): Flow<SubscriptionOperationMessage> {
        logger.error("Error parsing the subscription message $operationMessage", exception)
        return when (operationMessage) {
            is SubscriptionOperationMessage.OperationMessageId -> flowOf(
                SubscriptionOperationMessage.Error(
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
        session: WebSocketSession,
        text: String
    ): Flow<SubscriptionOperationMessage> {
        session.close(CloseReason(4400, text))
        return emptyFlow()
    }
}
