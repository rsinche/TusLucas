import java.util.Properties

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

    // Llave de firma: keystore.properties y el .jks no se suben a git
    val firma = rootProject.file("keystore.properties")
    if (firma.exists()) {
        val datos = Properties().apply { firma.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = file(datos.getProperty("storeFile"))
            storePassword = datos.getProperty("storePassword")
            keyAlias = datos.getProperty("keyAlias")
            keyPassword = datos.getProperty("keyPassword")
        }
    }

    buildTypes {
        // Misma llave en debug y release: las dos versiones se instalan una encima de otra sin perder datos
        debug {
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
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
