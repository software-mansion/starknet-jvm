plugins {
    application
}

dependencies {
    implementation("com.swmansion.starknet:starknet:0.13.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks.test{
    useJUnitPlatform()
}

application {
    mainClass.set("com.example.javademo.Main")
}
