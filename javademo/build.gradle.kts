plugins {
    application
}

dependencies {
//    testImplementation(project(mapOf("path" to ":lib")))
    implementation("com.swmansion.starknet:starknet:0.7.2")
}

application {
    mainClass.set("com.example.javademo.Main")
}
