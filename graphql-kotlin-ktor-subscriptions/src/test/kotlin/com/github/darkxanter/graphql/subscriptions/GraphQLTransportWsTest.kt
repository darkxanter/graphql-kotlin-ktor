package com.github.darkxanter.graphql.subscriptions

import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLTransportWsSubscriptionOperationMessage
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalCoroutinesApi
class GraphQLTransportWsTest {
    @Test
    fun `connection init`() = testApp { client ->
        client.subscription {
            sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit())
            receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck>()
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
            receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck>()
            delay(defaultConnectionInitWaitTimeout.plus(100.milliseconds))
            assertFalse(incoming.isClosedForReceive, "Connection must be opened")
        }
    }

    @Test
    fun `connection init more than one`() = testApp { client ->
        client.subscription {
            sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit())
            receiveDeserialized<GraphQLTransportWsSubscriptionOperationMessage.ConnectionAck>()
            sendSerialized(GraphQLTransportWsSubscriptionOperationMessage.ConnectionInit())
            withTimeout(500.milliseconds) {
                val reason = closeReason.await()
                assertEquals(4429, reason?.code)
                assertEquals("Too many initialisation requests", reason?.message)
            }
        }
    }
}

