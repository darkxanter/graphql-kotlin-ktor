package com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.darkxanter.graphql.subscriptions.SubscriptionWebSocketHandler

/**
 * WebSocket handler for handling `graphql-ws` protocol.
 * */
public class GraphQLWsSubscriptionWebSocketHandler(
    subscriptionHandler: GraphQLWsSubscriptionProtocolHandler,
    objectMapper: ObjectMapper = jacksonObjectMapper(),
) : SubscriptionWebSocketHandler<SubscriptionOperationMessage>(
    subscriptionHandler, objectMapper
) {
    override val protocol: String = "graphql-ws"
}
