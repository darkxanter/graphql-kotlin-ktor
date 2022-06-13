package com.github.darkxanter

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `java-library`
    `maven-publish`
    signing
}

val javaVersion = JavaVersion.VERSION_11

java {
    withSourcesJar()
//    withJavadocJar()
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    explicitApi()
}

dependencies {
    val logbackVersion: String by project

    implementation(platform(kotlin("bom")))

    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }

    test {
        useJUnitPlatform()
        testLogging {
            events(
                org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
            )
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

publishing {
    publications {
        create<MavenPublication>("mavenCentral") {
            from(components["java"])
            artifact(javadocJar)

            pom {
                name.set("Kotlock")
                afterEvaluate {
                    this@pom.description.set(project.description)
                }
                url.set("https://github.com/darkxanter/kotlock")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/mit-license.php")
                    }
                }
                scm {
                    url.set("https://github.com/darkxanter/kotlock")
                    connection.set("scm:git:git://github.com/darkxanter/kotlock.git")
                    developerConnection.set("scm:git:git@github.com:darkxanter/kotlock.git")
                }
                developers {
                    developer {
                        name.set("Sergey Shumov")
                        email.set("sergey0001@gmail.com")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = project.properties["mavenCentralUser"].toString()
                password = project.properties["mavenCentralPassword"].toString()
            }
        }
    }
}

//signing {
//    useGpgCmd()
//    sign(publishing.publications["mavenCentral"])
//}
