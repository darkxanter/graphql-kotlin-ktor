package example.plugins

import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
import com.github.darkxanter.graphql.GraphQLKotlin
import example.SubscriptionHooks
import example.feature.articles.ArticleMutation
import example.feature.articles.ArticleQuery
import example.feature.articles.ArticleRepository
import example.feature.articles.ArticleSubscription
import example.feature.users.User
import example.feature.users.UserDataLoader
import example.feature.users.UserQueryService
import example.feature.users.UserRepository
import example.graphql.CustomDataFetcherExceptionHandler
import example.graphql.CustomDirectiveWiringFactory
import example.graphql.HelloQueryService
import example.graphql.SimpleSubscription
import example.graphql.scalars.graphQLLong
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.websocket.WebSockets
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.time.Duration.Companion.seconds

fun Application.configureGraphQLModule() {
    val articleRepository: ArticleRepository by closestDI().instance()
    val userRepository: UserRepository by closestDI().instance()

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
            UserQueryService(userRepository),
            ArticleQuery(articleRepository),
        )
        mutations = listOf(
            ArticleMutation(articleRepository),
        )
        subscriptions = listOf(
            SimpleSubscription(),
            ArticleSubscription(articleRepository),
        )
        dataLoaders = listOf(
            UserDataLoader(userRepository),
        )

        subscriptionHooks = SubscriptionHooks()
        subscriptionPingInterval = 30.seconds

        schemaGeneratorConfig {
            supportedPackages = listOf("example")
            hooks = object : FlowSubscriptionSchemaGeneratorHooks() {
                override val wiringFactory: KotlinDirectiveWiringFactory
                    get() = CustomDirectiveWiringFactory()

                override fun willGenerateGraphQLType(type: KType): GraphQLType? {
                    return when (type.classifier as? KClass<*>) {
                        Long::class -> graphQLLong
                        OffsetDateTime::class -> ExtendedScalars.DateTime
                        ZonedDateTime::class -> ExtendedScalars.DateTime
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
            val loggedInUser = userId?.let { userRepository.findUserById(userId) }
            buildMap {
                loggedInUser?.let {
                    put(User::class, loggedInUser)
                }
            }
        }
    }
}
