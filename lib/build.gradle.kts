/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin library project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.2/userguide/building_java_projects.html
 */

import org.jetbrains.dokka.gradle.DokkaTask

version = "0.1.2"
group = "com.swmansion.starknet"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("org.jmailen.kotlinter")

    kotlin("plugin.serialization")

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
    signing
}

val dokkaHtmlJava by tasks.register("dokkaHtmlJava", DokkaTask::class) {
    dokkaSourceSets.create("dokkaHtmlJava") {
        dependencies {
            plugins("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.10")
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    moduleName.set("starknet-jvm")
    dokkaSourceSets {
        configureEach {
            includes.from("starknet-jvm.md")
        }
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

val buildCryptoCpp = task<Exec>("BuildCryptoCpp") {
    commandLine("${project.projectDir}/build_crypto_cpp.sh")
}

val compileContracts = task<Exec>("compileContracts") {
    commandLine("${project.projectDir}/src/test/resources/compileContracts.sh")
}

// For tests, we simply use version from crypto build
// For jars we use version from lib/build/libs/native
tasks.test {
    dependsOn(buildCryptoCpp)
    dependsOn(compileContracts)

    useJUnitPlatform()

    systemProperty("java.library.path", file("$buildDir/libs/shared").absolutePath)
    systemProperty("java.library.path", file("${rootDir}/crypto/build/bindings").absolutePath)

    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
        showStandardStreams = true
    }
}

kotlinter {
    disabledRules = arrayOf("no-wildcard-imports")
}

// Used by CI. Locally you should use jarWithNative task
tasks.jar {
    from(
        file("file:${buildDir}/libs/shared").absolutePath
    )
}

val jarWithNative = task("jarWithNative") {
    dependsOn(buildCryptoCpp)
    finalizedBy(tasks.jar)
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the JUnit test library.
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    // https://mvnrepository.com/artifact/org.mockito.kotlin/mockito-kotlin
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")

    // Crypto provider
    // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk15on
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}


publishing {
    publications {
        create<MavenPublication>("starknet") {
            artifactId = "starknet"
            artifact("starknet-jar/starknet.jar")
            artifact("starknet-aar/starknet.aar")
            artifact("javadoc-jar/javadoc.jar") {
                classifier="javadoc"
            }
            artifact("sources-jar/sources.jar"){
                classifier="sources"
            }
        pom {
            name.set("starknet")
            description.set("StarkNet SDK for JVM languages")
            url.set("https://github.com/software-mansion/starknet-jvm")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/software-mansion/starknet-jvm/blob/main/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("jakubptak")
                    name.set("Jakub Ptak")
                    email.set("jakub.ptak@swmansion.com")
                }
                developer {
                    id.set("arturmichalek")
                    name.set("Artur Michałek")
                    email.set("artur.michalek@swmansion.com")
                }
                developer {
                    id.set("bartoszrybarski")
                    name.set("Bartosz Rybarski")
                    email.set("bartosz.rybarski@swmansion.com")
                }
                developer {
                    id.set("wojciechszymczyk")
                    name.set("Wojciech Szymczyk")
                    email.set("wojciech.szymczyk@swmansion.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/software-mansion/starknet-jvm.git")
                developerConnection.set("scm:git:ssh://github.com:software-mansion/starknet-jvm.git")
                url.set("https://github.com/software-mansion/starknet-jvm/tree/main")
            }
        pom.withXml {
            val dependencyNode = asNode().appendNode("dependencies")

            val kotlinDependency = dependencyNode.appendNode("dependency")
            kotlinDependency.appendNode("groupId", "org.jetbrains.kotlin")
            kotlinDependency.appendNode("artifactId", "kotlin-stdlib-jdk8")
            kotlinDependency.appendNode("version", "1.3.50")

            val bouncyCastleDependency = dependencyNode.appendNode("dependency")
            bouncyCastleDependency.appendNode("groupId", "org.bouncycastle")
            bouncyCastleDependency.appendNode("artifactId", "bcprov-jdk15on")
            bouncyCastleDependency.appendNode("version", "1.70")

            val okHttpDependency = dependencyNode.appendNode("dependency")
            okHttpDependency.appendNode("groupId", "com.squareup.okhttp3")
            okHttpDependency.appendNode("artifactId", "okhttp")
            okHttpDependency.appendNode("version", "4.10.0")

            val kotlinxDependency = dependencyNode.appendNode("dependency")
            kotlinxDependency.appendNode("groupId", "org.jetbrains.kotlinx")
            kotlinxDependency.appendNode("artifactId", "kotlinx-serialization-json")
            kotlinxDependency.appendNode("version", "1.3.3")
            }
        }
    }
}

    repositories {
        maven {
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }

    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["starknet"])
}
