package com.github.darkxanter.graphql.subscriptions

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.websocket.WebSocketSession

/**
 * Ktor specific code to generate the context for a subscription request
 */
public abstract class KtorSubscriptionGraphQLContextFactory<out T : GraphQLContext> :
    GraphQLContextFactory<T, WebSocketSession>

/**
 * Basic implementation of [KtorSubscriptionGraphQLContextFactory] that just returns null
 */
public class DefaultKtorSubscriptionGraphQLContextFactory : KtorSubscriptionGraphQLContextFactory<GraphQLContext>() {
    @Deprecated("The generic context object is deprecated in favor of the context map")
    override suspend fun generateContext(request: WebSocketSession): GraphQLContext? = null
}
