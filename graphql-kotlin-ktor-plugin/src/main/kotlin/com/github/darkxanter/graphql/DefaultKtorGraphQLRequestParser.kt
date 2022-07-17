package com.github.darkxanter.graphql

import com.expediagroup.graphql.server.execution.GraphQLRequestParser
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receive
import java.io.IOException

/**
 * Default logic for how Ktor parses the incoming [ApplicationCall] into the [GraphQLServerRequest]
 */
public class DefaultKtorGraphQLRequestParser : GraphQLRequestParser<ApplicationCall> {
    override suspend fun parseRequest(request: ApplicationCall): GraphQLServerRequest = try {
        request.receive()
    } catch (e: IOException) {
        throw IOException("Unable to parse GraphQL payload.", e)
    }
}
