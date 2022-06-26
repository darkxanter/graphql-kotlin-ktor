package com.github.darkxanter.graphql.subscriptions

import com.expediagroup.graphql.server.types.GraphQLRequest
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLRequestWS
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLWsSubscriptionOperationMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class GraphQLWsTest {
    private suspend fun HttpClient.subscription(
        block: suspend DefaultClientWebSocketSession.() -> Unit
    ) = subscription("graphql-ws", block)

    private suspend fun DefaultClientWebSocketSession.initConnection() {
        withTimeout(500.milliseconds) {
            sendSerialized(
                GraphQLWsSubscriptionOperationMessage(
                    GraphQLWsSubscriptionOperationMessage.ClientMessages.GQL_CONNECTION_INIT.type
                )
            )
            val response = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
            assertEquals(GraphQLWsSubscriptionOperationMessage.ServerMessages.GQL_CONNECTION_ACK.type, response.type)
        }
    }

    @Test
    fun `connection init`() = testApp { client ->
        client.subscription {
            initConnection()
        }
    }

    @Test
    fun subscribe() = testApp { client ->
        val subscribeRequest = GraphQLWsSubscriptionOperationMessage(
            type = GraphQLWsSubscriptionOperationMessage.ClientMessages.GQL_START.type,
            id = "1",
            payload = GraphQLRequestWS(
                query = """
                    subscription Test {
                        twoValues
                    }
                """.trimIndent()
            )
        )

        client.subscription {
            initConnection()
            sendSerialized(subscribeRequest)

            withTimeout(500.milliseconds) {
                val response = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                assertEquals(subscribeRequest.id, response.id)
                assertEquals(GraphQLWsSubscriptionOperationMessage.ServerMessages.GQL_DATA.type, response.type)
                @Suppress("UNCHECKED_CAST")
                assertEquals(1, (response.payload as Map<String, Map<String, Int>>)["data"]?.get("twoValues"))
            }

            withTimeout(500.milliseconds) {
                val response = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                assertEquals(subscribeRequest.id, response.id)
                assertEquals(GraphQLWsSubscriptionOperationMessage.ServerMessages.GQL_DATA.type, response.type)
                @Suppress("UNCHECKED_CAST")
                assertEquals(2, (response.payload as Map<String, Map<String, Int>>)["data"]?.get("twoValues"))
            }

            withTimeout(500.milliseconds) {
                val complete = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                assertEquals(subscribeRequest.id, complete.id)
                assertEquals(GraphQLWsSubscriptionOperationMessage.ServerMessages.GQL_COMPLETE.type, complete.type)
            }
        }
    }

    @Test
    fun `subscription cancellation`() = testApp { client ->
        val subscribeRequest = GraphQLWsSubscriptionOperationMessage(
            type = GraphQLWsSubscriptionOperationMessage.ClientMessages.GQL_START.type,
            id = "1",
            payload = GraphQLRequestWS(
                query = """
                        subscription Test {
                            delayedValue(seconds: 5)
                        }
                    """.trimIndent()
            )
        )
        client.subscription {
            initConnection()

            withTimeout(5.seconds + 500.milliseconds) {
                sendSerialized(subscribeRequest)
                receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
            }

            assertFailsWith<TimeoutCancellationException> {
                withTimeout(500.milliseconds) {
                    sendSerialized(subscribeRequest)
                    sendSerialized(GraphQLWsSubscriptionOperationMessage(
                        type = GraphQLWsSubscriptionOperationMessage.ClientMessages.GQL_STOP.type,
                        id = subscribeRequest.id
                    ))
                    val response = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                    assertEquals(subscribeRequest.id, response.id)
                    assertEquals(GraphQLWsSubscriptionOperationMessage.ServerMessages.GQL_COMPLETE.type, response.type)
                }

                withTimeout(5.seconds + 500.milliseconds) {
                    val response = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                    println(response)
                    assert(false) {
                        "Operation must be cancelled"
                    }
                }
            }
        }
    }

    @Test
    fun `subscribe with error`() = testApp { client ->
        val subscribeRequest = GraphQLWsSubscriptionOperationMessage(
            type = GraphQLWsSubscriptionOperationMessage.ClientMessages.GQL_START.type,
            id = "1",
            payload = GraphQLRequest(
                query = """
                    subscription Test {
                        singleValueThenError
                    }
                """.trimIndent()
            )
        )

        client.subscription {
            initConnection()
            sendSerialized(subscribeRequest)

            withTimeout(500.milliseconds) {
                val response = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                assertEquals(subscribeRequest.id, response.id)
                assertEquals(
                    GraphQLWsSubscriptionOperationMessage.ServerMessages.GQL_DATA.type,
                    response.type,
                    "response $response",
                )
            }

            withTimeout(500.milliseconds) {
                val response = receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                assertEquals(subscribeRequest.id, response.id)
                assertEquals(GraphQLWsSubscriptionOperationMessage.ServerMessages.GQL_ERROR.type, response.type)
            }

            assertFailsWith<TimeoutCancellationException> {
                withTimeout(500.milliseconds) {
                    receiveDeserialized<GraphQLWsSubscriptionOperationMessage>()
                }
            }
        }
    }
}
