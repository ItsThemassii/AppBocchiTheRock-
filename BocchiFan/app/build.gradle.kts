plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.bocchifan.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bocchifan.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 101
        versionName = "1.0.1"
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // Crea la lista de idiomas (español / inglés) para el selector de idioma por app.
        generateLocaleConfig = true
        // Audio sin comprimir dentro del APK para poder reproducirlo directamente.
        noCompress += listOf("flac")
    }

    sourceSets {
        getByName("main") {
            // Carpeta "contenido" del proyecto: música (OST) e imágenes que pone el usuario.
            assets.srcDir("../../contenido")
        }
    }

    testOptions {
        unitTests {
            // Capturas de pantalla con Robolectric (sin emulador).
            isIncludeAndroidResources = true
            // En Windows, las pruebas descargan Robolectric usando el almacén de certificados del sistema.
            if (System.getProperty("os.name").startsWith("Windows")) {
                all { it.systemProperty("javax.net.ssl.trustStoreType", "Windows-ROOT") }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
