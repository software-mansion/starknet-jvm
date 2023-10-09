plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jmailen.kotlinter")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "com.example.androiddemo"
        minSdk = 24
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default values for demo purposes
        // You can manually change these values or set them via environment variables
        buildConfigField("String", "RPC_URL", "\"${System.getenv("RPC_URL") ?: "http://example-node-url.com/rpc"}\"")
        buildConfigField("String", "ACCOUNT_ADDRESS", "\"${System.getenv("ACCOUNT_ADDRESS") ?: "0x12345"}\"")
        buildConfigField("String", "PRIVATE_KEY", "\"${System.getenv("PRIVATE_KEY") ?: "0x123"}\"")
        buildConfigField("String", "RECIPIENT_ACCOUNT_ADDRESS", "\"${System.getenv("RECIPIENT_ACCOUNT_ADDRESS") ?: "0x789"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kotlinter {
    disabledRules = arrayOf("no-wildcard-imports")
}

val buildCrypto = task<Exec>("BuildCrypto") {
    commandLine("${project.projectDir}/build_crypto.sh")
}

tasks.withType<Test>().configureEach {
    dependsOn(buildCrypto)

    doFirst {
        val libsSharedPath = file("$buildDir/libs/shared").absolutePath
        val pedersenPath = file("${rootDir}/crypto/pedersen/build/bindings").absolutePath
        val poseidonPath = file("${rootDir}/crypto/poseidon/build/bindings").absolutePath
        systemProperty("java.library.path", "$libsSharedPath:$pedersenPath:$poseidonPath")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Make sure you are using the AAR and not a JAR and include transitive dependencies
        implementation("com.swmansion.starknet:starknet:0.7.2@aar"){
        isTransitive = true
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
