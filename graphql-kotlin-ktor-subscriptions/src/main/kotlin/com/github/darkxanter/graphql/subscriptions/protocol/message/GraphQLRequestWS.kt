package com.github.darkxanter.graphql.subscriptions.protocol.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import graphql.ExecutionInput
import org.dataloader.DataLoaderRegistry

/**
 * Wrapper that holds single GraphQLRequest to be processed within an WebSocket request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public data class GraphQLRequestWS(
    val query: String,
    val operationName: String? = null,
    val variables: Map<String, Any?>? = null,
    val extensions: Map<String, Any?>? = null,
)

/**
 * Convert the common [GraphQLRequestWS] to the execution input used by graphql-java
 */
public fun GraphQLRequestWS.toExecutionInput(
    dataLoaderRegistry: DataLoaderRegistry? = null,
    graphQLContextMap: Map<*, Any>? = null
): ExecutionInput =
    ExecutionInput.newExecutionInput()
        .query(query)
        .operationName(operationName)
        .variables(variables ?: emptyMap())
        .extensions(extensions ?: emptyMap())
        .also { builder ->
            graphQLContextMap?.let { builder.graphQLContext(it) }
        }
        .dataLoaderRegistry(dataLoaderRegistry ?: DataLoaderRegistry())
        .build()
