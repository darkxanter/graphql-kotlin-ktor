package com.github.darkxanter.graphql.subscriptions

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import com.github.darkxanter.graphql.GraphQLKotlin
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonWebsocketContentConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import io.ktor.util.KtorDsl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

val defaultConnectionInitWaitTimeout = 500.milliseconds

@KtorDsl
fun testApp(block: suspend ApplicationTestBuilder.(client: HttpClient) -> Unit) = testApplication {
    application { testModule() }
    val client = createClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            jackson()
        }
        install(io.ktor.client.plugins.websocket.WebSockets) {
            contentConverter = JacksonWebsocketContentConverter()

        }
    }
    block(client)
}

suspend fun HttpClient.subscription(
    protocol: String = "graphql-transport-ws",
    block: suspend DefaultClientWebSocketSession.() -> Unit
) = webSocket("/subscriptions", {
    header(HttpHeaders.SecWebSocketProtocol, protocol)
}, block)

fun Application.testModule() {
    install(ContentNegotiation) {
        jackson()
    }
    install(CallLogging)
    install(WebSockets)
    install(GraphQLKotlin) {
        queries = listOf(
            HelloQueryService()
        )

        subscriptions = listOf(
            SimpleSubscription()
        )

        subscriptionConnectionInitWaitTimeout = defaultConnectionInitWaitTimeout
    }
}

@Suppress("unused", "FunctionOnlyReturningConstant")
class HelloQueryService : Query {
    fun hello() = "World!"
}

class SimpleSubscription : Subscription {
    @GraphQLDescription("Returns a single value")
    fun singleValueSubscription(): Flow<Int> = flowOf(1)

    @GraphQLDescription("Returns one value then an error")
    fun singleValueThenError(): Flow<Int> = flowOf(1, 2)
        .map { if (it == 2) error("Second value") else it }

    @GraphQLDescription("Returns stream of errors")
    fun flowOfErrors(): Flow<DataFetcherResult<String?>> {
        val dfr: DataFetcherResult<String?> = DataFetcherResult.newResult<String?>()
            .data(null)
            .error(GraphqlErrorException.newErrorException().cause(Exception("error thrown")).build())
            .build()
        return flowOf(dfr, dfr)
    }
}
