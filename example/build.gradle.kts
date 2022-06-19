plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("example.ApplicationKt")
}

dependencies {
    implementation(projects.graphqlKotlinKtorPlugin)
//    implementation(projects.graphqlKotlinKtorSubscriptions)

    implementation(libs.ktor.server.allPlugins)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.jackson)
    implementation(libs.graphqlKotlin.server)
    implementation(libs.logback)
}
