package com.github.darkxanter.graphql.subscriptions

import io.ktor.server.routing.Route
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * WebSocket handler for handling GraphQL subscriptions.
 */
public abstract class SubscriptionWebSocketHandler<TMessage>(
    private val subscriptionHandler: SubscriptionProtocolHandler<TMessage>,
) {
    private val logger: Logger = LoggerFactory.getLogger(SubscriptionWebSocketHandler::class.java)
    public abstract val protocol: String

    public suspend fun handle(session: WebSocketServerSession) {
        try {
            subscriptionHandler.onConnect(session)
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val request = frame.readText()
                        logger.trace("request $request")
                        subscriptionHandler.handle(request, session)
                    }
                    else -> {}
                }
            }
        } finally {
            subscriptionHandler.onDisconnect(session)
        }
    }
}

public fun SubscriptionWebSocketHandler<*>.webSocket(
    route: Route,
    path: String,
) {
    route.webSocket(path, protocol = protocol) {
        handle(this)
    }
}
