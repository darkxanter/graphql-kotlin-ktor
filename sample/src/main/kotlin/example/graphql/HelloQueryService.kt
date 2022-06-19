package example.graphql

import com.expediagroup.graphql.server.operations.Query
import example.AuthorizedContext
import graphql.schema.DataFetchingEnvironment

class HelloQueryService : Query {
    fun hello() = "World!"

    fun helloWithContext(dfe: DataFetchingEnvironment): String {
        val authContext = dfe.graphQlContext.get<AuthorizedContext>("AuthorizedContext")
        return "Hello ${authContext.authorizedUser?.firstName}!"
    }
}
