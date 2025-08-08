allprojects {
    version = "0.16.0-SNAPSHOT"
    group = "com.swmansion.starknet"
}

plugins {
    id("com.android.library") version "7.4.0" apply false
    id("com.android.application") version "7.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
    id("org.jetbrains.dokka") version "1.8.20" apply false
    id("org.jmailen.kotlinter") version "3.15.0"
    id("org.jetbrains.kotlinx.kover") version "0.7.2"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}