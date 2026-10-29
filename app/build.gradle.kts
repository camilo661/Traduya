plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.traduya"

    defaultConfig {
        applicationId = "com.traduya.app"
        targetSdk = 35
        versionCode = 6
        versionName = "1.3.0"

        // Solo procesadores de teléfonos reales: reduce el APK a la mitad
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Firmado con la clave de depuración para poder instalarlo directamente en un teléfono de prueba
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.telecom)
    implementation(projects.core.translation)
    implementation(projects.core.audio)
    implementation(projects.core.callcapture)
    implementation(projects.core.session)

    implementation(projects.feature.onboarding)
    implementation(projects.feature.dialer)
    implementation(projects.feature.incall)
    implementation(projects.feature.history)
    implementation(projects.feature.settings)
    implementation(projects.feature.internetcall)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
