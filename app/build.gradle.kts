plugins {
    id("com.android.application")
}

android {
    namespace = "pe.yapevoz"
    compileSdk = 37

    defaultConfig {
        applicationId = "pe.yapevoz"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // FileProvider: compartir el CSV exportado con otras apps
    implementation("androidx.core:core:1.13.1")
    testImplementation("junit:junit:4.13.2")
}
