package com.github.darkxanter.graphql.subscriptions

import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLRequestWS
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLTransportWsSubscriptionOperationMessage
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class GraphQLTransportWsTest {
    private suspend fun DefaultClientWebSocketSession.initConnection() {
        withTimeout(500.milliseconds) {
            sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit())
            receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck>()
        }
    }

    @Test
    fun `connection init`() = testApp { client ->
        client.subscription {
            initConnection()
        }
    }

    @Test
    fun `connection init timeout`() = testApp { client ->
        client.subscription {
            delay(defaultConnectionInitWaitTimeout.plus(100.milliseconds))
            assert(incoming.isClosedForReceive) {
                "Connection must be closed"
            }
            val reason = closeReason.await()
            assertEquals(4408, reason?.code)
            assertEquals("Connection initialisation timeout", reason?.message)
        }

        client.subscription {
            delay(200.milliseconds)
            sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit())
            withTimeout(500.milliseconds) {
                receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck>()
            }
            delay(defaultConnectionInitWaitTimeout.plus(100.milliseconds))
            assertFalse(incoming.isClosedForReceive, "Connection must be opened")
        }
    }

    @Test
    fun `connection init more than one`() = testApp { client ->
        client.subscription {
            withTimeout(500.milliseconds) {
                sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit())
                receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck>()
                sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit())
                val reason = closeReason.await()
                assertEquals(4429, reason?.code)
                assertEquals("Too many initialisation requests", reason?.message)
            }
        }
    }

    @Test
    fun `connection init required`() = testApp { client ->
        val subscribeRequest = GraphQLTransportWsSubscriptionOperationMessage.Subscribe(
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
            withTimeout(500.milliseconds) {
                sendSerialized(subscribeRequest)
                val reason = closeReason.await()
                assertEquals(4401, reason?.code)
                assertEquals("Unauthorized", reason?.message)
            }
        }
    }


    @Test
    fun `server ping`() = testApp(pingInterval = 1.seconds) { client ->
        client.subscription {
            initConnection()

            withTimeout(250.milliseconds) {
                receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Ping>()
                sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.Pong())
            }

            withTimeout(1.seconds + 100.milliseconds) {
                receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Ping>()
                sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.Pong())
            }

            delay(600.milliseconds)
            assertTrue(!incoming.isClosedForReceive)

            withTimeout(1.seconds + 100.milliseconds) {
                receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Ping>()
            }

            delay(600.milliseconds)
            assertTrue(incoming.isClosedForReceive)

            withTimeout(200.milliseconds) {
                val reason = closeReason.await()
                assertEquals(4400, reason?.code)
                assertEquals("Pong timeout", reason?.message)
            }
        }
    }

    @Test
    fun `client ping`() = testApp { client ->
        client.subscription {
            initConnection()

            withTimeout(500.milliseconds) {
                sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.Ping())
                receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Pong>()
            }
        }
    }

    @Test
    fun subscribe() = testApp { client ->
        val subscribeRequest = GraphQLTransportWsSubscriptionOperationMessage.Subscribe(
            id = "1",
            payload = GraphQLRequestWS(
                query = """
                    subscription Test {
                        twoValues
                    }
                """.trimIndent()
            )
        )

        data class SubscriptionResponse(
            val twoValues: Int,
        )

        client.subscription {
            initConnection()
            sendSerialized(subscribeRequest)

            withTimeout(500.milliseconds) {
                val response1 =
                    receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Next<SubscriptionResponse>>()
                assertEquals(subscribeRequest.id, response1.id)
                assertEquals(1, response1.payload.data?.twoValues)
            }

            withTimeout(500.milliseconds) {
                val response2 =
                    receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Next<SubscriptionResponse>>()
                assertEquals(subscribeRequest.id, response2.id)
                assertEquals(2, response2.payload.data?.twoValues)
            }

            withTimeout(500.milliseconds) {
                val complete = receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Complete>()
                assertEquals(subscribeRequest.id, complete.id)
            }
        }
    }

    @Test
    fun `subscribe duplicate`() {
        testApp { client ->
            val subscribeRequest = GraphQLTransportWsSubscriptionOperationMessage.Subscribe(
                id = "42",
                payload = GraphQLRequestWS(
                    query = """
                        subscription Test {
                            counter
                        }
                    """.trimIndent()
                )
            )
            client.subscription {
                initConnection()

                withTimeout(500.milliseconds) {
                    sendSerialized(subscribeRequest)
                    receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Next<Any>>()
                    sendSerialized(subscribeRequest)

                    val reason = closeReason.await()
                    assertEquals(4409, reason?.code)
                    assertEquals("Subscriber for 42 already exists", reason?.message)
                }
            }
        }
    }

    @Test
    fun `subscription cancellation`() {
        testApp { client ->
            val subscribeRequest = GraphQLTransportWsSubscriptionOperationMessage.Subscribe(
                id = "1",
                payload = GraphQLRequestWS(
                    query = """
                        subscription Test {
                            delayedValue
                        }
                    """.trimIndent()
                )
            )
            client.subscription {
                initConnection()

                withTimeout(1.seconds + 500.milliseconds) {
                    sendSerialized(subscribeRequest)
                    receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Next<Any>>()
                    receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Complete>()
                }

                assertFailsWith<TimeoutCancellationException> {
                    withTimeout(1.seconds + 500.milliseconds) {
                        sendSerialized(subscribeRequest)
                        sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.Complete(subscribeRequest.id))

                        val response = receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage>()
                        println(response)
                        assert(false) {
                            "Operation must be cancelled"
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `subscribe with error`() = testApp { client ->
        val subscribeRequest = GraphQLTransportWsSubscriptionOperationMessage.Subscribe(
            id = "1",
            payload = GraphQLRequestWS(
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
                val response = receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Next<Any>>()
                assertEquals(subscribeRequest.id, response.id)
            }

            withTimeout(500.milliseconds) {
                val response = receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.Error>()
                assertEquals(subscribeRequest.id, response.id)
            }

            assertFailsWith<TimeoutCancellationException> {
                withTimeout(500.milliseconds) {
                    receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage>()
                }
            }
        }
    }
}
