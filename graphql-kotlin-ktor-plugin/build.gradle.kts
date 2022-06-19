plugins {
    id("com.github.darkxanter.library-convention")
}

dependencies {
    api(projects.graphqlKotlinKtorSubscriptions)
    api(libs.ktor.server.core)
    api(libs.graphqlKotlin.server)

    testImplementation(libs.bundles.test)
    testImplementation(libs.ktor.server.tests)
}
