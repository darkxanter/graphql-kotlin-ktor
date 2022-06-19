rootProject.name = "graphql-kotlin-ktor"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    val kotlinVersion: String by settings
    val gradleNexusPublishVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version kotlinVersion
        id("io.github.gradle-nexus.publish-plugin") version gradleNexusPublishVersion
    }
}

includeBuild("build-logic")
include(":graphql-kotlin-ktor-plugin")
include(":graphql-kotlin-ktor-subscriptions")
include(":example")
