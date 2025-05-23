plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.juno"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.juno"
        minSdk = 24
        targetSdk = 34
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

    // Add support for vector drawables
    buildFeatures {
        viewBinding = true
    }

    // Enable vector drawable support
    defaultConfig {
    }

    dependencies {
        // Firebase dependencies
        implementation("com.google.firebase:firebase-auth:22.3.1")
        implementation("com.google.firebase:firebase-database:20.3.0")
        implementation("com.google.android.gms:play-services-auth:20.7.0")
        implementation("com.google.firebase:firebase-analytics:21.5.0")
        implementation("com.google.firebase:firebase-storage:20.3.0")

        // Image handling
        implementation("com.github.bumptech.glide:glide:4.16.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
        implementation("com.firebaseui:firebase-ui-storage:8.0.2")
        implementation("de.hdodenhof:circleimageview:3.1.0")
        
        // Networking
        implementation("com.squareup.okhttp3:okhttp:4.11.0")
        implementation("com.android.volley:volley:1.2.1")
        
        // JSON processing
        implementation("com.google.code.gson:gson:2.10.1")

        implementation(libs.appcompat)
        implementation(libs.material)
        implementation(libs.activity)
        implementation(libs.constraintlayout)
        implementation(libs.firebase.database)
        testImplementation(libs.junit)
        androidTestImplementation(libs.ext.junit)
        androidTestImplementation(libs.espresso.core)
    }
}
dependencies {
    implementation(libs.volley)
}
