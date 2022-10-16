package example.feature.users

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import example.feature.auth.Role
import example.feature.auth.directives.Auth

@GraphQLDescription("User account")
data class User(
    val id: Long,
    @GraphQLDescription("Display name")
    val name: String,
    val username: String,
    @GraphQLIgnore
    val password: String,
    @Auth(Role.Admin, Role.Manager)
    @GraphQLDescription("User role")
    val role: Role,
)
