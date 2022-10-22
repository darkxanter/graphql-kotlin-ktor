package example

import example.plugins.configureDI
import example.plugins.configureDatabase
import example.plugins.configureGraphQLModule
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing


fun main() {
    embeddedServer(Netty, port = 4000, host = "0.0.0.0") {
        configureDatabase()
        configureDI()
        configureGraphQLModule()

        routing {
            get("/") {
                call.respondRedirect("/playground", permanent = false)
            }
        }
    }.start(wait = true)
}

