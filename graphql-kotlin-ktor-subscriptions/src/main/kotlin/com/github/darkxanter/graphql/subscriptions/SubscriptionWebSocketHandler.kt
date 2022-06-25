package com.github.darkxanter.graphql.subscriptions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.routing.Route
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * WebSocket handler for handling GraphQL subscriptions.
 */
public abstract class SubscriptionWebSocketHandler<TMessage>(
    private val subscriptionHandler: SubscriptionProtocolHandler<TMessage>,
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) {
    private val logger: Logger = LoggerFactory.getLogger(SubscriptionWebSocketHandler::class.java)
    public abstract val protocol: String

    public suspend fun handle(
        session: WebSocketServerSession,
        context: CoroutineContext = Dispatchers.IO,
    ): Unit = supervisorScope {
        try {
            subscriptionHandler.onConnect(session)
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        launch(context) {
                            val request = frame.readText()
                            logger.trace("request $request")
                            subscriptionHandler.handle(request, session).collect { message ->
                                val response = objectMapper.writeValueAsString(message)
                                logger.trace("response $response")
                                session.send(Frame.Text(response))
                            }
                        }
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
    context: CoroutineContext = Dispatchers.IO,
) {
    route.webSocket(path, protocol = protocol) {
        handle(this, context)
    }
}
