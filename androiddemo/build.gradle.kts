plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jmailen.kotlinter")
}

android {
    namespace = "com.example.androiddemo"
    compileSdk = 32

    defaultConfig {
        applicationId = "com.example.androiddemo"
        minSdk = 24
        targetSdk = 32
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

dependencies {
    implementation("androidx.core:core-ktx:1.8.0") // highest version available for sdk 32
    implementation("androidx.appcompat:appcompat:1.5.1") // highest version available for sdk 32
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Make sure you are using the AAR and not a JAR and include transitive dependencies
    implementation("com.swmansion.starknet:starknet:0.8.2@aar"){
        isTransitive = true
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
