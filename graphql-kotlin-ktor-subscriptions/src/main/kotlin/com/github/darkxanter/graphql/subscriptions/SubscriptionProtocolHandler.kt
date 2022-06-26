package com.github.darkxanter.graphql.subscriptions

import io.ktor.server.websocket.WebSocketServerSession

public interface SubscriptionProtocolHandler<TMessage> {
    public suspend fun handle(payload: String, session: WebSocketServerSession)
    public suspend fun onConnect(session: WebSocketServerSession)
    public suspend fun onDisconnect(session: WebSocketServerSession)
}
