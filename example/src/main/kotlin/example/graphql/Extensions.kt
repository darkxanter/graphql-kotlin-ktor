package example.graphql

import example.feature.auth.exceptions.UnauthorizedException
import example.feature.users.User
import graphql.GraphQLContext

val GraphQLContext.maybeUser: User?
    get() = get<User>(User::class)

val GraphQLContext.user: User
    get() = maybeUser ?: throw UnauthorizedException()
