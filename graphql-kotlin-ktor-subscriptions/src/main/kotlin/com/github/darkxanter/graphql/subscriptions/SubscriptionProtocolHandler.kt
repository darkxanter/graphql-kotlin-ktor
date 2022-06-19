package com.github.darkxanter.graphql.subscriptions

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.flow.Flow

public interface SubscriptionProtocolHandler<TMessage> {
    public suspend fun handle(payload: String, session: WebSocketServerSession): Flow<TMessage>
    public suspend fun onDisconnect(session: WebSocketServerSession)
}
