package com.github.darkxanter.graphql

import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 4000, host = "0.0.0.0") {

    }.start(wait = true)
}
