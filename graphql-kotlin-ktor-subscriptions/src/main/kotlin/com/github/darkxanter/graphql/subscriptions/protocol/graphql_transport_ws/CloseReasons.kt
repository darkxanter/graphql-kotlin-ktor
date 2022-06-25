package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import io.ktor.websocket.CloseReason

@Suppress("MagicNumber")
public object CloseReasons {
    public val connectionInitTimeout: CloseReason = CloseReason(4408, "Connection initialisation timeout")
    public val tooManyInitRequests: CloseReason = CloseReason(4429, "Too many initialisation requests")
}
