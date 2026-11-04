plugins {
    application
}

dependencies {
    implementation("com.swmansion.starknet:starknet:0.18.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.1.3")
}

tasks.test{
    useJUnitPlatform()
}

application {
    mainClass.set("com.example.javademo.Main")
}
