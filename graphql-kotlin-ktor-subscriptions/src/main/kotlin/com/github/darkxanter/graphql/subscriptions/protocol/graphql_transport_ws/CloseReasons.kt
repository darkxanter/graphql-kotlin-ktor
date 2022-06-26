package com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws

import io.ktor.websocket.CloseReason

@Suppress("MagicNumber")
public object CloseReasons {
    public val unauthorized: CloseReason = CloseReason(4401, "Unauthorized")
    public val connectionInitTimeout: CloseReason = CloseReason(4408, "Connection initialisation timeout")
    public val tooManyInitRequests: CloseReason = CloseReason(4429, "Too many initialisation requests")
    public fun subscriptionAlreadyExists(id: String): CloseReason {
        return CloseReason(4409, "Subscriber for $id already exists")
    }
    public fun invalidMessage(text: String): CloseReason = CloseReason(4400, text)
}
