package com.github.darkxanter.graphql.subscriptions

import com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws.SubscriptionOperationMessage
import io.ktor.server.websocket.WebSocketServerSession

/**
 * Implementation of Apollo Subscription Server Lifecycle Events
 * https://www.apollographql.com/docs/graphql-subscriptions/lifecycle-events/
 */
public interface ApolloSubscriptionHooks {
    /**
     * Allows validation of connectionParams prior to starting the connection.
     * You can reject the connection by throwing an exception.
     * If you need to forward state to execution, update and return the context map.
     */
    public fun onConnect(
        connectionParams: Map<String, String>,
        session: WebSocketServerSession,
        graphQLContext: Map<*, Any>?
    ): Map<*, Any>? = graphQLContext

    /**
     * Called when the client executes a GraphQL operation.
     * The context can not be updated here, it is read only.
     */
    public fun onOperation(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketServerSession,
        graphQLContext: Map<*, Any>?
    ): Unit = Unit

    /**
     * Called when client's unsubscribes
     */
    public fun onOperationComplete(session: WebSocketServerSession): Unit = Unit

    /**
     * Called when the client disconnects
     */
    public fun onDisconnect(session: WebSocketServerSession): Unit = Unit
}

/**
 * Default implementation of Apollo Subscription Lifecycle Events.
 */
public open class SimpleSubscriptionHooks : ApolloSubscriptionHooks
