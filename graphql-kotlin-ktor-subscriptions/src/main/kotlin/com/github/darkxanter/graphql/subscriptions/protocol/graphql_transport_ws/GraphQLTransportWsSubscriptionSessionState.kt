package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLTransportWsSubscriptionOperationMessage
import com.github.darkxanter.graphql.subscriptions.protocol.message.id
import io.ktor.websocket.CloseReason
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.ConcurrentHashMap

internal class GraphQLTransportWsSubscriptionSessionState {
    private val sessions = ConcurrentHashMap.newKeySet<WebSocketSession>()

    /** Sessions are saved by web socket session id */
    private val activeKeepAliveSessions = ConcurrentHashMap<WebSocketSession, Job>()

    /** Operations are saved by web socket session id, then operation id */
    private val activeOperations = ConcurrentHashMap<WebSocketSession, ConcurrentHashMap<String, Job>>()

    /** The graphQL context is saved by web socket session id */
    private val cachedGraphQLContext = ConcurrentHashMap<WebSocketSession, Map<*, Any>>()

    fun initialized(session: WebSocketSession) = sessions.add(session)
    fun isInitialized(session: WebSocketSession) = sessions.contains(session)

    /**
     * Save the context created from the factory and possibly updated in the onConnect hook.
     * This allows us to include some initial state to be used when handling all the messages.
     * This will be removed in [terminateSession].
     */
    fun saveContextMap(session: WebSocketSession, graphQLContext: Map<*, Any>?) {
        if (graphQLContext != null) {
            cachedGraphQLContext[session] = graphQLContext
        }
    }

    /**
     * Return the graphQL context for this session.
     */
    fun getGraphQLContext(session: WebSocketSession): Map<*, Any>? = cachedGraphQLContext[session]

    /**
     * Save the session that is sending keep alive messages.
     * This will override values without cancelling the subscription, so it is the responsibility of the consumer to cancel.
     * These messages will be stopped on [terminateSession].
     */
    fun saveKeepAliveSubscription(session: WebSocketSession, subscription: Job) {
        activeKeepAliveSessions[session] = subscription
    }

    /**
     * Save the operation that is sending data to the client.
     * This will override values without cancelling the subscription, so it is the responsibility of the consumer to cancel.
     * These messages will be stopped on [stopOperation].
     */
    fun saveOperation(
        session: WebSocketSession,
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage,
        subscription: Job
    ) {
        val id = operationMessage.id
        if (id != null) {
            val operationsForSession: ConcurrentHashMap<String, Job> =
                activeOperations.getOrPut(session) { ConcurrentHashMap() }
            operationsForSession[id] = subscription
        }
    }

    /**
     * Send the [GraphQLTransportWsSubscriptionOperationMessage.Complete] message.
     * This can happen when the publisher finishes or if the client manually sends the stop message.
     */
    fun completeOperation(
        session: WebSocketSession,
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        return getCompleteMessage(operationMessage)
            .onCompletion { removeActiveOperation(session, operationMessage.id) }
    }

    /**
     * Stop the subscription sending data and send the [GraphQLTransportWsSubscriptionOperationMessage.Complete] message.
     * Does NOT terminate the session.
     */
    fun stopOperation(
        session: WebSocketSession,
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage
    ): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        return getCompleteMessage(operationMessage)
            .onCompletion { removeActiveOperation(session, operationMessage.id) }
    }

    private fun getCompleteMessage(operationMessage: GraphQLTransportWsSubscriptionOperationMessage): Flow<GraphQLTransportWsSubscriptionOperationMessage> {
        val id = operationMessage.id
        if (id != null) {
            return flowOf(GraphQLTransportWsSubscriptionOperationMessage.Complete(id))
        }
        return emptyFlow()
    }

    /**
     * Remove active running subscription from the cache and cancel if needed
     */
    private fun removeActiveOperation(session: WebSocketSession, id: String?) {
        val operationsForSession = activeOperations[session]
        val subscription = operationsForSession?.get(id)
        if (subscription != null) {
            subscription.cancel()
            operationsForSession.remove(id)
            if (operationsForSession.isEmpty()) {
                activeOperations.remove(session)
            }
        }
    }

    /**
     * Terminate the session, cancelling the keep alive messages and all operations active for this session.
     */
    suspend fun terminateSession(
        session: WebSocketSession,
        reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, ""),
    ) {
        sessions.remove(session)
        activeOperations[session]?.forEach { (_, subscription) -> subscription.cancel() }
        activeOperations.remove(session)
        cachedGraphQLContext.remove(session)
        activeKeepAliveSessions[session]?.cancel()
        activeKeepAliveSessions.remove(session)
        session.close(reason)
    }

    /**
     * Looks up the operation for the client, to check if it already exists
     */
    fun doesOperationExist(
        session: WebSocketSession,
        operationMessage: GraphQLTransportWsSubscriptionOperationMessage
    ): Boolean =
        activeOperations[session]?.containsKey(operationMessage.id) ?: false
}
