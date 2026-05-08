plugins {
    id("tng.android.application")
    alias(libs.plugins.compose.compiler)
}

android {
    defaultConfig {
        applicationId = "com.samco.trackandgraph.changelogviewer"
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    namespace = "com.samco.trackandgraph.changelogviewer"
}

dependencies {
    implementation(project(":ui"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
}
