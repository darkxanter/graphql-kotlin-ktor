package com.github.darkxanter.graphql.subscriptions

import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.server.extensions.toExecutionInput
import com.expediagroup.graphql.server.extensions.toGraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.GraphQLResponse
import com.github.darkxanter.graphql.subscriptions.protocol.message.GraphQLRequestWS
import com.github.darkxanter.graphql.subscriptions.protocol.message.toExecutionInput
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Default Ktor implementation of GraphQL subscription handler.
 */
public open class KtorGraphQLSubscriptionHandler(
    private val graphQL: GraphQL,
    private val dataLoaderRegistryFactory: KotlinDataLoaderRegistryFactory? = null
) {
    public fun executeSubscription(
        graphQLRequest: GraphQLRequest,
        graphQLContextMap: Map<*, Any>? = null
    ): Flow<GraphQLResponse<*>> {
        val dataLoaderRegistry = dataLoaderRegistryFactory?.generate()
        val input = graphQLRequest.toExecutionInput(
            dataLoaderRegistry = dataLoaderRegistry,
            graphQLContextMap = graphQLContextMap,
        )
        return execute(input)
    }

    public fun executeSubscription(
        graphQLRequest: GraphQLRequestWS,
        graphQLContextMap: Map<*, Any>? = null
    ): Flow<GraphQLResponse<*>> {
        val dataLoaderRegistry = dataLoaderRegistryFactory?.generate()
        val input = graphQLRequest.toExecutionInput(
            dataLoaderRegistry = dataLoaderRegistry,
            graphQLContextMap = graphQLContextMap,
        )
        return execute(input)
    }

    private fun execute(input: ExecutionInput): Flow<GraphQLResponse<*>> {
        val executionResult = graphQL.execute(input)
        val data = executionResult.getData<Any>()
        @Suppress("UNCHECKED_CAST")
        return when (data) {
            is Flow<*> -> (data as Flow<ExecutionResult>).map { result -> result.toGraphQLResponse() }
            else -> flowOf(executionResult.toGraphQLResponse())
        }
    }
}
