import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jmailen.kotlinter")
}

android {
    namespace = "com.example.androiddemo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.androiddemo"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default values for demo purposes
        // You can manually change these values to interact with other networks
        buildConfigField("String", "DEMO_RPC_URL", "\"http://10.0.2.2:5050/rpc\"")
        buildConfigField("String", "DEMO_ACCOUNT_ADDRESS", "\"0x1323cacbc02b4aaed9bb6b24d121fb712d8946376040990f2f2fa0dcf17bb5b\"")
        buildConfigField("String", "DEMO_PRIVATE_KEY", "\"0xa2ed22bb0cb0b49c69f6d6a8d24bc5ea\"")
        buildConfigField("String", "DEMO_RECIPIENT_ACCOUNT_ADDRESS", "\"0xc1c7db92d22ef773de96f8bde8e56c85\"")
    }

    buildFeatures {
        buildConfig = true
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx) // requires sdk >= 32
    implementation(libs.androidx.appcompat) // requires sdk >= 32
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    // Make sure you are using the AAR and not a JAR and include transitive dependencies
    implementation("com.swmansion.starknet:starknet:${libs.versions.starknet.get()}@aar"){
        isTransitive = true
    }
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
