import com.android.build.api.dsl.Packaging

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
    fun Packaging.() {
        resources {
            excludes += "mockito-extensions/org.mockito.plugins.MockMaker"
        }
    }

}

dependencies {
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.zxing:core:3.3.3")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    // Firebase libraries (version managed by BOM)
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")

//    implementation(platform("com.google.firebase:firebase-storage:20.2.1"))
    implementation("com.google.guava:guava:31.1-android"){
        exclude( group= "com.google.protobuf", module= "protobuf-java")
    }
    implementation(libs.work.runtime)
    implementation(libs.firebase.database)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.location)
    implementation(libs.firebase.storage)
    implementation(libs.espresso.intents)
    implementation(libs.espresso.contrib)
//    implementation(libs.rules)
//    implementation("androidx.test:rules:1.4.0")
    testImplementation(libs.junit)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testImplementation("org.assertj:assertj-core:3.17.2")
    testImplementation("org.mockito:mockito-core:3.5.13")

    var camerax_version = "1.1.0" // Use the latest stable version if possible
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation ("androidx.camera:camera-camera2:$camerax_version")
    implementation ("androidx.camera:camera-lifecycle:$camerax_version")
    implementation ("androidx.camera:camera-view:$camerax_version") // For PreviewView
    implementation ("androidx.camera:camera-extensions:$camerax_version")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.rules)
    // Mockito for Android Instrumented Tests
//    androidTestImplementation("org.mockito:mockito-android:3.12.4")

    // Dexmaker dependencies for mocking final classes
//    androidTestImplementation("org.mockito:mockito-core:3.12.4") // Add this line
    androidTestImplementation("com.linkedin.dexmaker:dexmaker-mockito-inline:2.28.1")
//    androidTestImplementation("com.linkedin.dexmaker:dexmaker-mockito:2.28.1")

//    androidTestImplementation("org.mockito:mockito-core:3.12.4")
//    androidTestImplementation("org.mockito:mockito-android:3.12.4")
//    androidTestImplementation("org.mockito:mockito-inline:3.12.4")
}
configurations {
    all {
        exclude(group= "com.google.protobuf", module= "protobuf-lite")
    }
}
