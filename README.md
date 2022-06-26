# GraphQL Kotlin Ktor Plugin

[![Continuous Integration](https://github.com/darkxanter/graphql-kotlin-ktor/workflows/ci/badge.svg)](https://github.com/darkxanter/graphql-kotlin-ktor/actions?query=workflow%3A%22ci%22)
![Maven Central](https://img.shields.io/maven-central/v/io.github.darkxanter.graphql/graphql-kotlin-ktor-plugin)

Ktor plugin for [graphql-kotlin](https://github.com/ExpediaGroup/graphql-kotlin).

Also provides subscriptions implementation for protocols `graphql-ws` and `graphql-transport-ws`.

## 📦 Modules

- [graphql-kotlin-ktor-plugin](/graphql-kotlin-ktor-plugin) - Ktor plugin
- [graphql-kotlin-ktor-subscriptions](/graphql-kotlin-ktor-subscriptions) - GraphQL subscriptions implementation for Ktor
- [example](/example) - Ktor server example

## Usage

```kotlin
dependencies {
    implementation("io.github.darkxanter.graphql", "graphql-kotlin-ktor-plugin", "<last-version>")
}
```

```kotlin
fun Application.configureGraphQLModule() {
    install(ContentNegotiation) {
        jackson()
    }
    install(CallLogging)
    install(WebSockets)
    install(GraphQLKotlin) {
        queries = listOf(
            HelloQueryService(),
        )
        subscriptions = listOf(
            SimpleSubscription()
        )

        schemaGeneratorConfig {
            supportedPackages = listOf("example.graphql")
        }

        generateContextMap { request ->
            val loggedInUser = User(
                email = "johndoe@example.com",
                firstName = "John",
                lastName = "Doe",
            )
            mapOf(
                "AuthorizedContext" to AuthorizedContext(loggedInUser)
            )
        }
    }
}
```
