package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import com.github.darkxanter.graphql.subscriptions.SubscriptionWebSocketHandler
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLTransportWsSubscriptionOperationMessage

/**
 * WebSocket handler for handling `graphql-transport-ws` protocol.
 * */
public class GraphQLTransportWsSubscriptionWebSocketHandler(
    subscriptionHandler: GraphQLTransportWsSubscriptionProtocolHandler,
) : SubscriptionWebSocketHandler<GraphQLTransportWsSubscriptionOperationMessage>(
    subscriptionHandler
) {
    override val protocol: String = "graphql-transport-ws"
}
