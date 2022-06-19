package example

import com.github.darkxanter.graphql.subscriptions.ApolloSubscriptionHooks
import com.github.darkxanter.graphql.subscriptions.protocol.message.SubscriptionOperationMessage
import io.ktor.server.websocket.WebSocketServerSession
import org.slf4j.LoggerFactory

class SubscriptionHooks : ApolloSubscriptionHooks {
    private val logger = LoggerFactory.getLogger(SubscriptionHooks::class.java)

    override fun onConnect(
        connectionParams: Map<String, String>,
        session: WebSocketServerSession,
        graphQLContext: Map<*, Any>?
    ): Map<*, Any>? {
        logger.debug("onConnect $session $graphQLContext $connectionParams")
        return graphQLContext
    }

    /**
     * Called when the client executes a GraphQL operation.
     * The context can not be updated here, it is read only.
     */
    override fun onOperation(
        operationMessage: SubscriptionOperationMessage,
        session: WebSocketServerSession,
        graphQLContext: Map<*, Any>?
    ) {
        logger.debug("onOperation $session $graphQLContext ${operationMessage.toString().replace("\n", " ")}")
    }

    /**
     * Called when client's unsubscribes
     */
    override fun onOperationComplete(session: WebSocketServerSession) {
        logger.debug("onOperationComplete $session")
    }

    /**
     * Called when the client disconnects
     */
    override fun onDisconnect(session: WebSocketServerSession) {
        logger.debug("onDisconnect $session")
    }
}
