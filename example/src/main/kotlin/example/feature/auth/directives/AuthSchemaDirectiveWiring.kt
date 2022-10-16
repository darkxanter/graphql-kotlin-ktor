package example.feature.auth.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import example.feature.auth.Role
import example.feature.auth.exceptions.ForbiddenException
import example.graphql.user
import graphql.schema.DataFetcher
import graphql.schema.DataFetcherFactories
import graphql.schema.GraphQLFieldDefinition
import org.slf4j.LoggerFactory

class AuthSchemaDirectiveWiring : KotlinSchemaDirectiveWiring {
    private val logger = LoggerFactory.getLogger(AuthSchemaDirectiveWiring::class.java)

    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val field = environment.element
        val targetAuthRoles = environment.directive.getArgument("roles").argumentValue.value?.let { roles ->
            if (roles !is Array<*>) {
                error("the roles field must be array")
            }
            roles.filterIsInstance<Role>()
        } ?: error("the roles field must exist in the auth directive")
        val originalDataFetcher: DataFetcher<*> = environment.getDataFetcher()
        val authorizationFetcherFetcher = DataFetcherFactories.wrapDataFetcher(originalDataFetcher) { dataEnv, value ->
            val user = dataEnv.graphQlContext.user
            logger.debug("userId=${user.id} role=${user.role} targetRoles=${targetAuthRoles}")
            if (targetAuthRoles.isNotEmpty() && targetAuthRoles.contains(user.role).not()) {
                throw ForbiddenException()
            }
            value
        }
        environment.setDataFetcher(authorizationFetcherFetcher)
        return field
    }
}
