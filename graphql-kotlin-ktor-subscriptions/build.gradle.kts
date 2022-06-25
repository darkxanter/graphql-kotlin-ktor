plugins {
    id("com.github.darkxanter.library-convention")
}

description = "GraphQL Subscriptions implementation for graphql-kotlin"

dependencies {
    api(libs.ktor.server.core)
    api(libs.ktor.server.websockets)
    api(libs.graphqlKotlin.server)

    testImplementation(projects.graphqlKotlinKtorPlugin)
    testImplementation(libs.ktor.jackson)
    testImplementation(libs.ktor.client.contentNegotiation)
    testImplementation(libs.ktor.client.websockets)

    testImplementation(libs.bundles.test)
    testImplementation(libs.ktor.server.tests)
}
