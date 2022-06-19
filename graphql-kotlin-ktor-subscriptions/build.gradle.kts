plugins {
    id("com.github.darkxanter.library-convention")
}

dependencies {
    api(libs.ktor.server.core)
    api(libs.ktor.server.websockets)
    api(libs.graphqlKotlin.server)

    testImplementation(libs.bundles.test)
    testImplementation(libs.ktor.server.tests)
}
