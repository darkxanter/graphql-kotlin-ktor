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
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(properties["mavenCentralUser"] as String)
            password.set(properties["mavenCentralPassword"] as String)
        }
    }
}
