plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}

// La carpeta del proyecto tiene "!" en el nombre y Java lo confunde con el separador de los .jar
// (rompe las pruebas). Por eso los archivos de compilación van a una carpeta sin "!".
if (rootDir.absolutePath.contains("!")) {
    val outside = File(System.getProperty("user.home"), ".bocchifan-build")
    allprojects {
        layout.buildDirectory.set(File(outside, project.name))
    }
}
