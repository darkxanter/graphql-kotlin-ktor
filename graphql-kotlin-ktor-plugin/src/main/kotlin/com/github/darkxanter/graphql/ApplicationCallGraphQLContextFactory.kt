@file:Suppress("DEPRECATION")

package com.github.darkxanter.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest

/**
 * Custom logic for how this app should create its context given the [ApplicationRequest]
 */

public interface ApplicationCallGraphQLContextFactory : GraphQLContextFactory<GraphQLContext, ApplicationCall> {
    @Deprecated("The generic context object is deprecated in favor of the context map", ReplaceWith("generateContextMap(request)"))
    override suspend fun generateContext(request: ApplicationCall): GraphQLContext? = null
}

public class DefaultApplicationCallGraphQLContextFactory : ApplicationCallGraphQLContextFactory
