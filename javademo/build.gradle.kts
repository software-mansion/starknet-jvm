plugins {
    application
}

dependencies {
    // TODO: remove this line once updated jar is published.
    // In a real project, use the following line instead:
    // implementation("com.swmansion.starknet:starknet:0.7.2")
    implementation(project(mapOf("path" to ":lib")))
}

application {
    mainClass.set("com.example.javademo.Main")
}

tasks.register<JavaExec>("runWithLocalJar") {
    group = "application"
    mainClass.set("com.example.javademo.Main")
    classpath = sourceSets["main"].runtimeClasspath
    val jarPath = file("${rootDir}/java/src/test/resources/starknet.jar").absolutePath
    systemProperty("com.swmansion.starknet.library.jarPath", jarPath)
}