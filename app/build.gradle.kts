plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.naufall.textdetection"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.naufall.textdetection"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    // Object detection feature with bundled default classifier
    implementation ("com.google.mlkit:object-detection:17.0.0")
    // Object detection feature with custom classifier support
    implementation ("com.google.mlkit:object-detection-custom:17.0.0")

    implementation ("androidx.multidex:multidex:2.0.1")
    // Camera
    implementation ("com.google.mlkit:camera:16.0.0-beta3")
    // Text features
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    // Or comment the dependency above and uncomment the dependency below to
}