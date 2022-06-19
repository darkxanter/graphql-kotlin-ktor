package example

import graphql.GraphQLException

/**
 * Example of a custom GraphQLContext
 */
data class AuthorizedContext(
    val authorizedUser: User? = null,
    val customHeader: String? = null
)

data class User(
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val isAdmin: Boolean = false
) {
    fun intThatNeverComes(): Int = throw GraphQLException("This value will never return")
}
