plugins {
    id("com.github.darkxanter.library-convention")
}

val ktorVersion: String by project
val graphqlKotlinVersion: String by project
val logbackVersion: String by project

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("com.expediagroup:graphql-kotlin-server:$graphqlKotlinVersion")

    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
}
