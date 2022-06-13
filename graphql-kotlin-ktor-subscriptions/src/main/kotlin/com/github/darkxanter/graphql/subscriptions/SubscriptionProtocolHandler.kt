package com.github.darkxanter.graphql.subscriptions

import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.flow.Flow

public interface SubscriptionProtocolHandler<TMessage> {
    public suspend fun handle(payload: String, session: WebSocketSession): Flow<TMessage>
}
