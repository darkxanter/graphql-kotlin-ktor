@file:Suppress("DEPRECATION")

package com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws

import com.expediagroup.graphql.server.types.GraphQLRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.darkxanter.graphql.subscriptions.*
import dev.xanter.graphql.subscription.protocol.graphql_ws.GraphQLWsSubscriptionSessionState
import com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws.SubscriptionOperationMessage.ClientMessages
import com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws.SubscriptionOperationMessage.ServerMessages
import io.ktor.websocket.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import org.slf4j.LoggerFactory

/**
 * Implementation of the `graphql-ws` protocol defined by Apollo
 * https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 */
public class GraphQLWsSubscriptionProtocolHandler(
    private val contextFactory: KtorSubscriptionGraphQLContextFactory<*>,
    private val subscriptionHandler: KtorGraphQLSubscriptionHandler,
    private val objectMapper: ObjectMapper,
    private val subscriptionHooks: ApolloSubscriptionHooks,
    private val keepAliveInterval: Long? = null,
): SubscriptionProtocolHandler<SubscriptionOperationMessage> {
    private val sessionState = GraphQLWsSubscriptionSessionState()
    private val logger = LoggerFactory.getLogger(GraphQLWsSubscriptionProtocolHandler::class.java)
    private val keepAliveMessage = SubscriptionOperationMessage(type = ServerMessages.GQL_CONNECTION_KEEP_ALIVE.type)
    private val basicConnectionErrorMessage =
        SubscriptionOperationMessage(type = ServerMessages.GQL_CONNECTION_ERROR.type)
    private val acknowledgeMessage = SubscriptionOperationMessage(ServerMessages.GQL_CONNECTION_ACK.type)

    @Suppress("Detekt.TooGenericExceptionCaught")
    override suspend fun handle(payload: String, session: WebSocketSession): Flow<SubscriptionOperationMessage> {
        val operationMessage = convertToMessageOrNull(payload) ?: return flowOf(basicConnectionErrorMessage)
        logger.debug("GraphQL subscription client, session=$session operationMessage=$operationMessage")

        return try {
            when (operationMessage.type) {
                ClientMessages.GQL_CONNECTION_INIT.type -> onInit(operationMessage, session)
                ClientMessages.GQL_START.type -> startSubscription(operationMessage, session)
                ClientMessages.GQL_STOP.type -> onStop(operationMessage, session)
                ClientMessages.GQL_CONNECTION_TERMINATE.type -> onDisconnect(session)
                else -> onUnknownOperation(operationMessage, session)
            }
        } catch (exception: Exception) {
            onException(exception)
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
    private fun getKeepAliveFlow(session: WebSocketSession): Flow<SubscriptionOperationMessage> {
        if (keepAliveInterval != null) {
            return flow {
                while (true) {
                    emit(keepAliveMessage)
                    delay(keepAliveInterval)
                }
            }.onStart {
                sessionState.saveKeepAliveSubscription(session, currentCoroutineContext().job)
            }
        }
        return emptyFlow()
    }

    @Suppress("Detekt.TooGenericExceptionCaught")
    private fun startSubscription(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession
    ): Flow<SubscriptionOperationMessage> {
        val context = sessionState.getContext(session)
        val graphQLContext = sessionState.getGraphQLContext(session)

        subscriptionHooks.onOperation(operationMessage, session, context)
        subscriptionHooks.onOperationWithContext(operationMessage, session, graphQLContext)

        if (operationMessage.id == null) {
            logger.error("GraphQL subscription operation id is required")
            return flowOf(basicConnectionErrorMessage)
        }

        if (sessionState.doesOperationExist(session, operationMessage)) {
            logger.info("Already subscribed to operation ${operationMessage.id} for session $session")
            return emptyFlow()
        }

        val payload = operationMessage.payload

        if (payload == null) {
            logger.error("GraphQL subscription payload was null instead of a GraphQLRequest object")
            sessionState.stopOperation(session, operationMessage)
            return flowOf(
                SubscriptionOperationMessage(
                    type = ServerMessages.GQL_CONNECTION_ERROR.type,
                    id = operationMessage.id
                )
            )
        }

        try {
            val request = objectMapper.convertValue<GraphQLRequest>(payload)
            return subscriptionHandler.executeSubscription(request, context, graphQLContext)
                .map {
                    if (it.errors?.isNotEmpty() == true) {
                        SubscriptionOperationMessage(
                            type = ServerMessages.GQL_ERROR.type,
                            id = operationMessage.id,
                            payload = it
                        )
                    } else {
                        SubscriptionOperationMessage(
                            type = ServerMessages.GQL_DATA.type,
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
                SubscriptionOperationMessage(
                    type = ServerMessages.GQL_CONNECTION_ERROR.type,
                    id = operationMessage.id
                )
            )
        }
    }

    private suspend fun onInit(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession
    ): Flow<SubscriptionOperationMessage> {
        saveContext(operationMessage, session)
        val acknowledgeMessage = flowOf(acknowledgeMessage)
        val keepAliveFlow = getKeepAliveFlow(session)
        return acknowledgeMessage.onCompletion { if (it == null) emitAll(keepAliveFlow) }
            .catch { emit(getConnectionErrorMessage(operationMessage)) }
    }

    /**
     * Generate the context and save it for all future messages.
     */
    private suspend fun saveContext(operationMessage: SubscriptionOperationMessage, session: WebSocketSession) {
        val connectionParams = castToMapOfStringString(operationMessage.payload)
        val context = contextFactory.generateContext(session)
        val graphQLContext = contextFactory.generateContextMap(session)
        val onConnectContext = subscriptionHooks.onConnect(connectionParams, session, context)
        val onConnectGraphQLContext =
            subscriptionHooks.onConnectWithContext(connectionParams, session, graphQLContext)
        sessionState.saveContext(session, onConnectContext)
        sessionState.saveContextMap(session, onConnectGraphQLContext)
    }

    /**
     * Called with the publisher has completed on its own.
     */
    private suspend fun onComplete(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession
    ): Flow<SubscriptionOperationMessage> {
        subscriptionHooks.onOperationComplete(session)
        return sessionState.completeOperation(session, operationMessage)
    }

    /**
     * Called with the client has called stop manually, or on error, and we need to cancel the publisher
     */
    private fun onStop(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession
    ): Flow<SubscriptionOperationMessage> {
        subscriptionHooks.onOperationComplete(session)
        return sessionState.stopOperation(session, operationMessage)
    }

    private suspend fun onDisconnect(session: WebSocketSession): Flow<SubscriptionOperationMessage> {
        subscriptionHooks.onDisconnect(session)
        sessionState.terminateSession(session)
        return emptyFlow()
    }

    private fun onUnknownOperation(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession
    ): Flow<SubscriptionOperationMessage> {
        logger.error("Unknown subscription operation $operationMessage")
        sessionState.stopOperation(session, operationMessage)
        return flowOf(getConnectionErrorMessage(operationMessage))
    }

    private fun onException(exception: Exception): Flow<SubscriptionOperationMessage> {
        logger.error("Error parsing the subscription message", exception)
        return flowOf(basicConnectionErrorMessage)
    }

    private fun getConnectionErrorMessage(operationMessage: SubscriptionOperationMessage): SubscriptionOperationMessage {
        return SubscriptionOperationMessage(type = ServerMessages.GQL_CONNECTION_ERROR.type, id = operationMessage.id)
    }
}
