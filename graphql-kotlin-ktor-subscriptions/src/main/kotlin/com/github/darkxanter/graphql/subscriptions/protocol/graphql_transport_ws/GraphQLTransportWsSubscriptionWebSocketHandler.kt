package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.darkxanter.graphql.subscriptions.SubscriptionWebSocketHandler
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLTransportWsSubscriptionOperationMessage

/**
 * WebSocket handler for handling `graphql-transport-ws` protocol.
 * */
public class GraphQLTransportWsSubscriptionWebSocketHandler(
    subscriptionHandler: GraphQLTransportWsSubscriptionProtocolHandler,
    objectMapper: ObjectMapper = jacksonObjectMapper(),
) : SubscriptionWebSocketHandler<GraphQLTransportWsSubscriptionOperationMessage>(
    subscriptionHandler, objectMapper
) {
    override val protocol: String = "graphql-transport-ws"
}
