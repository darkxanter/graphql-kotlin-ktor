package example.feature.auth.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import example.feature.auth.Role
import graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION

@Suppress("unused")
@GraphQLDirective(
    name = "auth",
    description = "Is Authenticated?",
    locations = [
        FIELD_DEFINITION
    ]
)
annotation class Auth(vararg val roles: Role)
