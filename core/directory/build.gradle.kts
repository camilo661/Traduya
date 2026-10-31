plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/** Lee una propiedad de gradle.properties (o -P en la línea de comandos) como literal de Java. */
fun quotedProperty(name: String): String = "\"" + providers.gradleProperty(name).getOrElse("") + "\""

android {
    namespace = "com.traduya.core.directory"
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "FIREBASE_PROJECT_ID", quotedProperty("traduya.firebase.projectId"))
        buildConfigField("String", "FIREBASE_API_KEY", quotedProperty("traduya.firebase.apiKey"))
        // Solo para pruebas con el emulador de Firebase (por ejemplo "10.0.2.2"); se ignora en release
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", quotedProperty("traduya.firebase.emulatorHost"))
    }
}

dependencies {
    implementation(projects.core.domain)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
