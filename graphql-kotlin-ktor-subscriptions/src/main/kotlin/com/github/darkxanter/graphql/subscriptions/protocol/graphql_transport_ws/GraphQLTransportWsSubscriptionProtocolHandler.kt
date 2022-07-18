package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import com.expediagroup.graphql.server.extensions.toGraphQLError
import com.expediagroup.graphql.server.extensions.toGraphQLKotlinType
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
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of the `graphql-transport-ws` protocol
 *
 * https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 */
public class GraphQLTransportWsSubscriptionProtocolHandler(
    private val contextFactory: GraphQLContextFactory<*, ApplicationCall>,
    private val subscriptionHandler: KtorGraphQLSubscriptionHandler,
    private val objectMapper: ObjectMapper,
    private val subscriptionHooks: ApolloSubscriptionHooks,
    connectionInitWaitTimeout: Duration,
    private val pingInterval: Duration = Duration.ZERO,
    subscriptionCoroutineContext: CoroutineContext = Dispatchers.Unconfined,
) : SubscriptionProtocolHandler<GraphQLTransportWsSubscriptionOperationMessage> {
    private val scope = CoroutineScope(subscriptionCoroutineContext + SupervisorJob())

    private val sessionState = GraphQLTransportWsSubscriptionSessionState(connectionInitWaitTimeout)
    private val pongs = ConcurrentHashMap<WebSocketServerSession, Job>()
    private val logger = LoggerFactory.getLogger(GraphQLTransportWsSubscriptionProtocolHandler::class.java)

    @Suppress("Detekt.TooGenericExceptionCaught")
    override suspend fun handle(payload: String, session: WebSocketServerSession) {
        val operationMessage = convertToMessageOrNull(payload)
        if (operationMessage == null) {
            closeWithInvalidMessage(
                session,
                "Unknown message or message with missing fields",
            )
            return
        }
        logger.trace("GraphQL subscription client, session=$session operationMessage=$operationMessage")
        try {
            val responseFlow: Flow<GraphQLTransportWsSubscriptionOperationMessage> = when (operationMessage) {
                is GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit -> onInit(operationMessage, session)
                is GraphQLTransportWsSubscriptionOperationMessage.Subscribe -> startSubscription(
                    operationMessage,
                    session
                )
                is GraphQLTransportWsSubscriptionOperationMessage.Complete -> {
                    sessionState.stopOperation(session, operationMessage)
                    return
                }
                is GraphQLTransportWsSubscriptionOperationMessage.Ping -> flowOf(
                    GraphQLTransportWsSubscriptionOperationMessage.Pong()
                )
                is GraphQLTransportWsSubscriptionOperationMessage.Pong -> {
                    pongs.remove(session)?.cancel()
                    return
                }
                is GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck,
                is GraphQLTransportWsSubscriptionOperationMessage.Next<*>,
                is GraphQLTransportWsSubscriptionOperationMessage.Error -> {
                    closeWithInvalidMessage(session, "Error message can be sent only from server")
                    return
                }
            }
            scope.launch {
                responseFlow.sendMessages(session)
            }
        } catch (exception: Exception) {
            onException(session, operationMessage, exception).sendMessages(session)
        }
    }

    private suspend fun Flow<GraphQLTransportWsSubscriptionOperationMessage>.sendMessages(session: WebSocketServerSession) {
        collect { message ->
            val response = objectMapper.writeValueAsString(message)
            session.send(Frame.Text(response))
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
                    pongs.remove(session)?.cancel()
                    pongs[session] = scope.launch {
                        delay(1.seconds)
                        sessionState.terminateSession(session, CloseReasons.pongTimeout)
                    }
                    delay(pingInterval)
                }
            }.onStart {
                sessionState.saveKeepAliveSubscription(session, currentCoroutineContext().job)
            }
        }
        return emptyFlow()
    }

    @Suppress("Detekt.TooGenericExceptionCaught")
    private suspend fun startSubscription(
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage.Subscribe,
        session: WebSocketServerSession
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        if (!sessionState.isInitialized(session)) {
            sessionState.terminateSession(session, CloseReasons.unauthorized)
            return emptyFlow()
        }

        if (sessionState.doesOperationExist(session, operationMessage)) {
            sessionState.terminateSession(session, CloseReasons.subscriptionAlreadyExists(operationMessage.id))
            return emptyFlow()
        }

        val graphQLContext = sessionState.getGraphQLContext(session)
        subscriptionHooks.onOperation(operationMessage, session, graphQLContext)

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
                .catch { throwable ->
                    val error = throwable.toGraphQLError()
                    emit(
                        GraphQLTransportWsSubscriptionOperationMessage.Error(
                            id = operationMessage.id,
                            payload = listOf(error.toGraphQLKotlinType()),
                        )
                    )
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
        if (sessionState.isInitialized(session)) {
            sessionState.terminateSession(session, CloseReasons.tooManyInitRequests)
            return flowOf()
        }
        sessionState.initialized(session)
        saveContext(operationMessage, session)
        val acknowledgeMessage = flowOf<GraphQLTransportWsSubscriptionOperationMessage>(
            GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck()
        )
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

    override suspend fun onConnect(session: WebSocketServerSession) {
        sessionState.newConnection(session)
    }

    override suspend fun onDisconnect(session: WebSocketServerSession) {
        subscriptionHooks.onDisconnect(session)
        sessionState.terminateSession(session)
    }

    private suspend fun onException(
        session: WebSocketServerSession,
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage,
        exception: Exception
    ) = flow<GraphQLTransportWsSubscriptionOperationMessage> {
        logger.error("Error parsing the subscription message $operationMessage", exception)
        when (operationMessage) {
            is GraphQLTransportWsSubscriptionOperationMessage.OperationMessageId -> {
                emit(
                    GraphQLTransportWsSubscriptionOperationMessage.Error(
                        operationMessage.id,
                        payload = listOf(
                            GraphQLServerError("Internal Error"),
                        )
                    )
                )
            }
            else -> closeWithInvalidMessage(session, "Internal Error")
        }
    }

    private suspend fun closeWithInvalidMessage(
        session: WebSocketServerSession,
        text: String
    ) {
        sessionState.terminateSession(session, CloseReasons.invalidMessage(text))
    }
}
