plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "kr.co.takeaseat.app"
    compileSdk = 37 // 최신 AndroidX(Compose 1.12)가 37 이상을 요구한다

    defaultConfig {
        applicationId = "kr.co.takeaseat.app" // iOS 번들 ID와 같게
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.6.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
