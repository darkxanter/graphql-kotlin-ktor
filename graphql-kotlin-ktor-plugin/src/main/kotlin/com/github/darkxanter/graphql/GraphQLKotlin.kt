package com.github.darkxanter.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelNames
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.execution.FlowSubscriptionExecutionStrategy
import com.expediagroup.graphql.generator.execution.KotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.extensions.print
import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
import com.expediagroup.graphql.generator.hooks.NoopSchemaGeneratorHooks
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.generator.toSchema
import com.expediagroup.graphql.server.execution.DataLoaderRegistryFactory
import com.expediagroup.graphql.server.execution.DefaultDataLoaderRegistryFactory
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLRequestParser
import com.expediagroup.graphql.server.execution.GraphQLServer
import com.expediagroup.graphql.server.execution.KotlinDataLoader
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.darkxanter.graphql.subscriptions.ApolloSubscriptionHooks
import com.github.darkxanter.graphql.subscriptions.KtorGraphQLSubscriptionHandler
import com.github.darkxanter.graphql.subscriptions.SimpleSubscriptionHooks
import com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws.GraphQLTransportWsSubscriptionProtocolHandler
import com.github.darkxanter.graphql.subscriptions.protocol.graphql_transport_ws.GraphQLTransportWsSubscriptionWebSocketHandler
import com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws.GraphQLWsSubscriptionProtocolHandler
import com.github.darkxanter.graphql.subscriptions.protocol.graphql_ws.GraphQLWsSubscriptionWebSocketHandler
import com.github.darkxanter.graphql.subscriptions.webSocket
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@Suppress("MemberVisibilityCanBePrivate")
public class GraphQLKotlin(private val config: GraphQLKotlinConfiguration) {
    public val graphqlEndpoint: String = config.endpoints.graphql
    public val sdlEndpoint: String = config.endpoints.sdl
    public val playgroundEndpoint: String = config.endpoints.playground
    public val subscriptionsEndpoint: String = config.endpoints.subscriptions

    public val graphQLSchema: GraphQLSchema = toSchema(
        config = config.schemaGeneratorConfig,
        queries = config.queries.toTopLevelObjects(),
        mutations = config.mutations.toTopLevelObjects(),
        subscriptions = config.subscriptions.toTopLevelObjects(),
    )

    public val dataLoaderRegistryFactory: DataLoaderRegistryFactory =
        DefaultDataLoaderRegistryFactory(config.dataLoaders)

    public val graphQL: GraphQL = GraphQL
        .newGraphQL(graphQLSchema)
        .subscriptionExecutionStrategy(FlowSubscriptionExecutionStrategy())
        .apply(config.configureGraphQL)
        .build()

    public val graphQLServer: KtorGraphQLServer = GraphQLServer(
        config.requestParser,
        config.contextFactory,
        GraphQLRequestHandler(graphQL, dataLoaderRegistryFactory),
    )

    private val callHandler: KtorGraphQLCallHandler = config.callHandler

    public companion object Plugin :
        BaseApplicationPlugin<Application, GraphQLKotlinConfiguration, GraphQLKotlin> {
        override val key: AttributeKey<GraphQLKotlin> = AttributeKey("GraphQLKotlin")
        override fun install(
            pipeline: Application,
            configure: GraphQLKotlinConfiguration.() -> Unit
        ): GraphQLKotlin {
            val plugin = GraphQLKotlin(GraphQLKotlinConfiguration().apply(configure))
            plugin.configureRouting(pipeline)
            if (plugin.config.endpoints.enableSubscriptions)
                plugin.configureSubscriptions(pipeline)
            return plugin
        }
    }

    private fun configureRouting(application: Application) {
        application.routing {
            val endpoints = config.endpoints

            if (endpoints.enableGraphql) {
                post(graphqlEndpoint) {
                    callHandler(graphQLServer, call)
                }
            }

            if (endpoints.enablePlayground) {
                get(playgroundEndpoint) {
                    call.respondText(
                        buildPlaygroundHtml(graphqlEndpoint, subscriptionsEndpoint),
                        ContentType.Text.Html
                    )
                }
            }

            if (endpoints.enableSdl) {
                get(sdlEndpoint) {
                    call.respondText(graphQLSchema.print())
                }
            }
        }
    }

    private fun configureSubscriptions(application: Application) {
        val subscriptionHooks = config.subscriptionHooks
        val subscriptionObjectMapper = config.subscriptionObjectMapper
        val subscriptionCoroutineContext = config.subscriptionCoroutineContext
        val subscriptionConnectionInitWaitTimeout = config.subscriptionConnectionInitWaitTimeout

        val graphQLWsSubscriptionProtocolHandler = GraphQLWsSubscriptionProtocolHandler(
            contextFactory = config.contextFactory,
            subscriptionHandler = KtorGraphQLSubscriptionHandler(graphQL, dataLoaderRegistryFactory),
            objectMapper = subscriptionObjectMapper,
            subscriptionHooks = subscriptionHooks,
        )

        val graphQLTransportWsSubscriptionProtocolHandler = GraphQLTransportWsSubscriptionProtocolHandler(
            contextFactory = config.contextFactory,
            subscriptionHandler = KtorGraphQLSubscriptionHandler(graphQL, dataLoaderRegistryFactory),
            objectMapper = subscriptionObjectMapper,
            subscriptionHooks = subscriptionHooks,
            connectionInitWaitTimeout = subscriptionConnectionInitWaitTimeout,
        )

        val graphQLWsSubscriptionHandler = GraphQLWsSubscriptionWebSocketHandler(
            graphQLWsSubscriptionProtocolHandler,
        )

        val graphQLTransportWsSubscriptionHandler = GraphQLTransportWsSubscriptionWebSocketHandler(
            graphQLTransportWsSubscriptionProtocolHandler,
        )

        application.routing {
            graphQLWsSubscriptionHandler.webSocket(this, subscriptionsEndpoint, subscriptionCoroutineContext)
            graphQLTransportWsSubscriptionHandler.webSocket(this, subscriptionsEndpoint, subscriptionCoroutineContext)
        }
    }
}

public class GraphQLKotlinConfiguration {
    public var queries: List<Query> = emptyList()
    public var mutations: List<Mutation> = emptyList()
    public var subscriptions: List<Subscription> = emptyList()
    public var schemaGeneratorConfig: SchemaGeneratorConfig = SchemaGeneratorConfig(
        supportedPackages = emptyList(),
        hooks = FlowSubscriptionSchemaGeneratorHooks()
    )

    public var subscriptionConnectionInitWaitTimeout: Duration = 3.seconds

    public fun schemaGeneratorConfig(configure: SchemaGeneratorConfigMutable.() -> Unit) {
        schemaGeneratorConfig = SchemaGeneratorConfigMutable(
            supportedPackages = emptyList(),
            hooks = FlowSubscriptionSchemaGeneratorHooks()
        ).apply(configure)
    }

    public var dataLoaders: List<KotlinDataLoader<*, *>> = emptyList()

    public var contextFactory: GraphQLContextFactory<*, ApplicationCall> =
        DefaultApplicationCallGraphQLContextFactory()

    public fun generateContextMap(block: GenerateContextMap) {
        contextFactory = object : ApplicationCallGraphQLContextFactory {
            override suspend fun generateContextMap(request: ApplicationCall): Map<*, Any>? = block(request)
        }
    }

    public var endpoints: Endpoints = Endpoints()
    public inline fun endpoints(crossinline configure: Endpoints.() -> Unit) {
        endpoints.apply(configure)
    }

    internal var configureGraphQL: GraphQL.Builder.() -> Unit = {}
    public fun configureGraphQL(configure: GraphQL.Builder.() -> Unit) {
        configureGraphQL = configure
    }

    public var subscriptionHooks: ApolloSubscriptionHooks = SimpleSubscriptionHooks()
    public var subscriptionObjectMapper: ObjectMapper = jacksonObjectMapper()
    public var subscriptionCoroutineContext: CoroutineContext = Dispatchers.IO

    public var requestParser: KtorGraphQLRequestParser = DefaultKtorGraphQLRequestParser()
    public fun requestParser(handler: KtorGraphQLRequestParserHandler) {
        requestParser = object : KtorGraphQLRequestParser {
            override suspend fun parseRequest(request: ApplicationCall): GraphQLServerRequest = handler(request)
        }
    }

    public var callHandler: KtorGraphQLCallHandler = { graphQlServer, call ->
        // Execute the query against the schema
        val result = graphQlServer.execute(call)
        if (result != null) {
            call.respond(result)
        } else {
            call.respond(HttpStatusCode.BadRequest, "Invalid request")
        }
    }

    public fun callHandler(handler: KtorGraphQLCallHandler) {
        callHandler = handler
    }

    public class Endpoints {
        public var graphql: String = "graphql"
        public var sdl: String = "sdl"
        public var playground: String = "playground"
        public var subscriptions: String = "subscriptions"
        public var enableGraphql: Boolean = true
        public var enableSdl: Boolean = true
        public var enablePlayground: Boolean = true
        public var enableSubscriptions: Boolean = true
    }

    public class SchemaGeneratorConfigMutable(
        override var supportedPackages: List<String>,
        override var topLevelNames: TopLevelNames = TopLevelNames(),
        override var hooks: SchemaGeneratorHooks = NoopSchemaGeneratorHooks,
        override var dataFetcherFactoryProvider: KotlinDataFetcherFactoryProvider = SimpleKotlinDataFetcherFactoryProvider(),
        override var introspectionEnabled: Boolean = true,
        override var additionalTypes: Set<GraphQLType> = emptySet()
    ) : SchemaGeneratorConfig(
        supportedPackages = supportedPackages,
        topLevelNames = topLevelNames,
        hooks = hooks,
        dataFetcherFactoryProvider = dataFetcherFactoryProvider,
        introspectionEnabled = introspectionEnabled,
        additionalTypes = additionalTypes,
    )
}

public typealias KtorGraphQLServer = GraphQLServer<ApplicationCall>
public typealias KtorGraphQLRequestParser = GraphQLRequestParser<ApplicationCall>
public typealias GenerateContextMap = (request: ApplicationCall) -> Map<Any, Any>?
public typealias KtorGraphQLCallHandler = suspend (graphQLServer: KtorGraphQLServer, call: ApplicationCall) -> Unit
public typealias KtorGraphQLRequestParserHandler = suspend (request: ApplicationCall) -> GraphQLServerRequest

private fun List<Any>.toTopLevelObjects() = map { TopLevelObject(it) }

@Suppress("SameParameterValue")
public fun buildPlaygroundHtml(graphQLEndpoint: String, subscriptionsEndpoint: String): String =
    Application::class.java.classLoader.getResource("graphql-playground.html")?.readText()
        ?.replace("\${graphQLEndpoint}", graphQLEndpoint)
        ?.replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
        ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")
