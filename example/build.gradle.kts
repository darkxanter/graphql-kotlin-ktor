@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    application
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

application {
    mainClass.set("example.ApplicationKt")
}

dependencies {
    implementation(projects.graphqlKotlinKtorPlugin)
//    implementation(projects.graphqlKotlinKtorSubscriptions)
    implementation(libs.graphqlKotlin.server)
    implementation(libs.graphqlJava.extendedScalars)
    implementation(libs.graphqlJava.extendedValidation)

    implementation(libs.ktor.server.allPlugins)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.jackson)
    implementation(libs.logback)
    implementation(libs.kotlinLogging)

    implementation(libs.sqlite)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.exposed.kotlinDatetime)

    implementation(libs.kodein)

    compileOnly(libs.kesp.annotations)
    ksp(libs.kesp.processor)
}

sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
}
