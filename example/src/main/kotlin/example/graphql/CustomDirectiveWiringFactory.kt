package example.graphql

import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import example.feature.auth.directives.Auth
import example.feature.auth.directives.AuthSchemaDirectiveWiring
import example.graphql.directives.Trim
import example.graphql.directives.TrimSchemaDirectiveWiring
import graphql.schema.GraphQLDirectiveContainer
import kotlin.reflect.KClass

class CustomDirectiveWiringFactory : KotlinDirectiveWiringFactory(
    manualWiring = mapOf(
        getDirectiveName(Auth::class) to AuthSchemaDirectiveWiring(),
        getDirectiveName(Trim::class) to TrimSchemaDirectiveWiring(),
    )
) {
//    private val validationRules = ValidationRules.newValidationRules()
//        .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
//        .build()
//    private val validationSchemaWiring = object : KotlinSchemaDirectiveWiring {
//        private val wiring = ValidationSchemaWiring(validationRules)
//        override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
//            return wiring.onField(environment)
//        }
//    }


    override fun getSchemaDirectiveWiring(environment: KotlinSchemaDirectiveEnvironment<GraphQLDirectiveContainer>): KotlinSchemaDirectiveWiring? =
        when (environment.directive.name) {
//            getDirectiveName(NotEmpty::class) -> validationSchemaWiring
            else -> null
        }
}

internal fun getDirectiveName(kClass: KClass<out Annotation>): String =
    kClass.simpleName!!.replaceFirstChar { it.lowercase() }
