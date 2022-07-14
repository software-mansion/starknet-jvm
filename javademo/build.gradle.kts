plugins {
    application
}

dependencies {
    implementation(project(mapOf("path" to ":lib")))
}

application {
    mainClass.set("com.example.javademo.Main")
}