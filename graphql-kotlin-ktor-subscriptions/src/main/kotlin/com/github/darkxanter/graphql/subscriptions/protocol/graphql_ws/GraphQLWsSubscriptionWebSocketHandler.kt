package com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws

import com.github.darkxanter.graphql.subscriptions.SubscriptionWebSocketHandler
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLWsSubscriptionOperationMessage

/**
 * WebSocket handler for handling `graphql-ws` protocol.
 * */
public class GraphQLWsSubscriptionWebSocketHandler(
    subscriptionHandler: GraphQLWsSubscriptionProtocolHandler,
) : SubscriptionWebSocketHandler<GraphQLWsSubscriptionOperationMessage>(
    subscriptionHandler
) {
    override val protocol: String = "graphql-ws"
}
