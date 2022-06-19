package com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.darkxanter.graphql.subscriptions.SubscriptionWebSocketHandler
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLWsSubscriptionOperationMessage

/**
 * WebSocket handler for handling `graphql-ws` protocol.
 * */
public class GraphQLWsSubscriptionWebSocketHandler(
    subscriptionHandler: GraphQLWsSubscriptionProtocolHandler,
    objectMapper: ObjectMapper = jacksonObjectMapper(),
) : SubscriptionWebSocketHandler<GraphQLWsSubscriptionOperationMessage>(
    subscriptionHandler, objectMapper
) {
    override val protocol: String = "graphql-ws"
}
