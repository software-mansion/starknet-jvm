plugins {
    application
}

dependencies {
    implementation(libs.starknet)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test{
    useJUnitPlatform()
}

application {
    mainClass.set("com.example.javademo.Main")
}
