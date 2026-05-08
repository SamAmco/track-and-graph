plugins {
    id("tng.android.library")
    alias(libs.plugins.compose.compiler)
}

android {
    buildFeatures {
        compose = true
    }

    namespace = "com.samco.trackandgraph.ui"
}

dependencies {
    implementation(libs.threetenabp)

    implementation(libs.core.ktx)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)

    implementation(libs.multiplatform.markdown.renderer)
    implementation(libs.multiplatform.markdown.renderer.m3)
    implementation(libs.multiplatform.markdown.renderer.coil3)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
