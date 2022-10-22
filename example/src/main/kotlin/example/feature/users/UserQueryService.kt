package example.feature.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import example.feature.auth.Role
import example.feature.auth.directives.Auth


class UserQueryService(private val userRepository: UserRepository) : Query {
    @Auth(Role.Admin, Role.Manager)
    @GraphQLDescription("List of users")
    fun users(): List<User> {
        return userRepository.list()
    }
}
