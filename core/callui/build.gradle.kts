plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.traduya.core.callui"
    buildFeatures { compose = true }
}

dependencies {
    api(projects.core.designsystem)
}
