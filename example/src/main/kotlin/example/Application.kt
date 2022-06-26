package example

import com.github.darkxanter.graphql.GraphQLKotlin
import example.graphql.HelloQueryService
import example.graphql.SimpleSubscription
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.websocket.WebSockets
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 4000, host = "0.0.0.0") {
        configureGraphQLModule()

    }.start(wait = true)
}

fun Application.configureGraphQLModule() {
    install(ContentNegotiation) {
        jackson()
    }

    install(CallLogging)
    install(WebSockets) {
//        pingPeriod = Duration.ofSeconds(15)
//        timeout = Duration.ofSeconds(15)
    }
    install(GraphQLKotlin) {
        queries = listOf(
            HelloQueryService(),
        )
        subscriptions = listOf(
            SimpleSubscription()
        )
        subscriptionHooks = SubscriptionHooks()
        subscriptionPingInterval = 30.seconds

        schemaGeneratorConfig {
            supportedPackages = listOf("example.graphql")
        }

        generateContextMap { request ->
            val loggedInUser = User(
                email = "johndoe@example.com",
                firstName = "John",
                lastName = "Doe",
            )

            // Parse any headers from the Ktor request
            val customHeader: String? = request.request.headers["my-custom-header"]

            mapOf(
                "AuthorizedContext" to AuthorizedContext(loggedInUser, customHeader = customHeader)
            )
        }
    }
}

