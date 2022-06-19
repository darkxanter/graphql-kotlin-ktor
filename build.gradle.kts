plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

println("version $version")

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(properties["mavenCentralUser"] as String)
            password.set(properties["mavenCentralPassword"] as String)
        }
    }
}
