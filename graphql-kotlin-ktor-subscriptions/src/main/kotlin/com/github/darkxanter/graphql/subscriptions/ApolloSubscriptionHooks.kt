package com.github.darkxanter.graphql.subscriptions

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws.SubscriptionOperationMessage
import io.ktor.websocket.WebSocketSession

/**
 * Implementation of Apollo Subscription Server Lifecycle Events
 * https://www.apollographql.com/docs/graphql-subscriptions/lifecycle-events/
 */
public interface ApolloSubscriptionHooks {

    /**
     * Allows validation of connectionParams prior to starting the connection.
     * You can reject the connection by throwing an exception.
     * If you need to forward state to execution, update and return the [GraphQLContext].
     */
    @Deprecated("The generic context object is deprecated in favor of the context map", ReplaceWith("onConnectWithContext"))
    public fun onConnect(
        connectionParams: Map<String, String>,
        session: WebSocketSession,
        graphQLContext: GraphQLContext?
    ): GraphQLContext? = graphQLContext

    /**
     * Allows validation of connectionParams prior to starting the connection.
     * You can reject the connection by throwing an exception.
     * If you need to forward state to execution, update and return the context map.
     */
    public fun onConnectWithContext(
        connectionParams: Map<String, String>,
        session: WebSocketSession,
        graphQLContext: Map<*, Any>?
    ): Map<*, Any>? = graphQLContext

    /**
     * Called when the client executes a GraphQL operation.
     * The context can not be updated here, it is read only.
     */
    @Deprecated("The generic context object is deprecated in favor of the context map", ReplaceWith("onOperationWithContext"))
    public fun onOperation(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession,
        graphQLContext: GraphQLContext?
    ): Unit = Unit

    /**
     * Called when the client executes a GraphQL operation.
     * The context can not be updated here, it is read only.
     */
    public fun onOperationWithContext(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketSession,
        graphQLContext: Map<*, Any>?
    ): Unit = Unit

    /**
     * Called when client's unsubscribes
     */
    public fun onOperationComplete(session: WebSocketSession): Unit = Unit

    /**
     * Called when the client disconnects
     */
    public fun onDisconnect(session: WebSocketSession): Unit = Unit
}

/**
 * Default implementation of Apollo Subscription Lifecycle Events.
 */
public open class SimpleSubscriptionHooks : ApolloSubscriptionHooks
