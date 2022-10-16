package example

import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
import com.github.darkxanter.graphql.GraphQLKotlin
import example.feature.auth.directives.AuthSchemaDirectiveWiring
import example.feature.users.User
import example.feature.users.UserQueryService
import example.feature.users.UserRepository
import example.graphql.CustomDataFetcherExceptionHandler
import example.graphql.HelloQueryService
import example.graphql.SimpleSubscription
import example.graphql.scalars.graphQLLong
import graphql.schema.GraphQLType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.websocket.WebSockets
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 4000, host = "0.0.0.0") {
        configureGraphQLModule()

    }.start(wait = true)
}

fun Application.configureGraphQLModule() {
    install(ContentNegotiation) {
        jackson()
    }

    install(CallLogging)
    install(WebSockets) {
//        pingPeriod = Duration.ofSeconds(15)
//        timeout = Duration.ofSeconds(15)
    }
    install(GraphQLKotlin) {
        queries = listOf(
            HelloQueryService(),
            UserQueryService(),
        )
        subscriptions = listOf(
            SimpleSubscription()
        )
        subscriptionHooks = SubscriptionHooks()
        subscriptionPingInterval = 30.seconds

        schemaGeneratorConfig {
            supportedPackages = listOf("example")
            hooks = object : FlowSubscriptionSchemaGeneratorHooks() {
                override val wiringFactory: KotlinDirectiveWiringFactory
                    get() = KotlinDirectiveWiringFactory(
                        mapOf(
                            "auth" to AuthSchemaDirectiveWiring()
                        ),
                    )

                override fun willGenerateGraphQLType(type: KType): GraphQLType? {
                    return when (type.classifier as? KClass<*>) {
                        Long::class -> graphQLLong
                        else -> null
                    }
                }
            }
        }

        configureGraphQL {
            defaultDataFetcherExceptionHandler(CustomDataFetcherExceptionHandler())
        }

        generateContextMap { request ->
            val userId = request.request.header("X-User-Id")?.toLong()
            val loggedInUser = userId?.let { UserRepository().findUserById(userId) }
            buildMap {
                loggedInUser?.let {
                    put(User::class, loggedInUser)
                }
            }
        }
    }
}
