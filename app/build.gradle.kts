plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.potato1_events"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.potato1_events"
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
    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("com.google.zxing:core:3.3.3")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation(platform("com.google.firebase:firebase-storage:20.2.1"))
    implementation ("com.google.guava:guava:29.0-android")
    implementation (libs.work.runtime)
    implementation (libs.firebase.database)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.location)
    implementation(libs.firebase.storage)
    //implementation(libs.rules)
    testImplementation(libs.junit)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testImplementation("org.assertj:assertj-core:3.17.2")
    testImplementation("org.mockito:mockito-core:3.5.13")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

