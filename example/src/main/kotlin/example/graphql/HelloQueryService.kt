package example.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import example.feature.auth.Role
import example.feature.auth.directives.Auth
import graphql.schema.DataFetchingEnvironment

class HelloQueryService : Query {
    @GraphQLDescription("Hello example")
    fun hello() = "World!"

    @Auth
    @GraphQLDescription("Hello example with auth context")
    fun helloWithContext(dfe: DataFetchingEnvironment): String {
        val user = dfe.graphQlContext.user
        return "Hello ${user.name}!"
    }


    @Auth(Role.Admin, Role.Manager)
    @GraphQLDescription("Hello example with auth context for admin users only")
    fun adminHelloWithContext(dfe: DataFetchingEnvironment): String {
        val user = dfe.graphQlContext.user
        return "Hello ${user.name}!"
    }
}
