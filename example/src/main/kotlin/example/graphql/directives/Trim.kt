package example.graphql.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import graphql.introspection.Introspection
import graphql.schema.DataFetcherFactories
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import mu.KotlinLogging
import java.util.*

@Suppress("unused")
@GraphQLDirective(
    name = "trim",
    locations = [
//        Introspection.DirectiveLocation.FIELD,
        Introspection.DirectiveLocation.FIELD_DEFINITION,
    ],
    description = "Trim spaces",
)
annotation class Trim

class TrimSchemaDirectiveWiring : KotlinSchemaDirectiveWiring {
    private val logger = KotlinLogging.logger {  }

    override fun wireOnEnvironment(environment: KotlinSchemaDirectiveEnvironment<*>): GraphQLDirectiveContainer {
        logger.debug("wireOnEnvironment ${environment.element}")
        return super.wireOnEnvironment(environment)
    }

    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val field = environment.element
        val originalDataFetcher = environment.getDataFetcher()
        val trimFetcher = DataFetcherFactories.wrapDataFetcher(
            originalDataFetcher
        ) { _, value ->
            logger.debug("onField value $value")
            when (value) {
                is String -> value.trim()
                else -> value
            }
        }
        environment.setDataFetcher(trimFetcher)
        return field
    }
//
//    override fun onInputObjectField(environment: KotlinSchemaDirectiveEnvironment<GraphQLInputObjectField>): GraphQLInputObjectField {
//        val field = environment.element
//        return field.transform {
//            logger.info { "onInputObjectField transform $it" }
//            it.name(field.name + "_test")
//        }
//    }

//    override fun onArgument(environment: KotlinSchemaDirectiveEnvironment<GraphQLArgument>): GraphQLArgument {
//        logger.debug("onArgument")
//        val argument = environment.element
//        return argument.transform {
//            val appliedArgument = argument.toAppliedArgument()
//            val argumentValue = appliedArgument.argumentValue.value
//            logger.debug("argumentValue $argumentValue")
//
//            it.defaultValueProgrammatic(argumentValue.toString().trim())
//        }

//        return when (argumentValue) {
//            is String -> argument.transform {
//                it.defaultValueProgrammatic(argumentValue.trim())
//            }
//
//            else -> argument
//        }
//    }
}
