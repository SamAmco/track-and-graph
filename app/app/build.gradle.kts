/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.Properties

plugins {
    id("tng.android.application")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.screenshot)
}

// Load local.properties if it exists
val localPropsFile: File? = rootProject.file("local.properties")
val localProps = Properties().apply {
    if (localPropsFile?.exists() == true) {
        load(localPropsFile.inputStream())
    }
}

android {
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        applicationId = "com.samco.trackandgraph"
        //If the backup file is not backwards compatible after this update, upgrade the major version number!
        versionCode = 800020
        versionName = "10.1.3"
        // Default manifest placeholder for RecreateAlarms receiver
        manifestPlaceholders["recreateAlarmsEnabled"] = "true"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = (localProps["KEYSTORE_FILE"] as? String)?.let { file(it) }
            storePassword = localProps["KEYSTORE_PASSWORD"] as String?
            keyAlias = localProps["KEY_ALIAS"] as String?
            keyPassword = localProps["KEY_PASSWORD"] as String?
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Debug Track & Graph")
            manifestPlaceholders["ALLOW_CLEAR_TEXT"] = "false"
            manifestPlaceholders["NETWORK_SECURITY_CONFIG"] = "@xml/debug_network_security_config"
            manifestPlaceholders["recreateAlarmsEnabled"] = "true"
        }
        create("debugMinify") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug.minify"
            resValue("string", "app_name", "Debug Minify Track & Graph")
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["ALLOW_CLEAR_TEXT"] = "false"
            manifestPlaceholders["NETWORK_SECURITY_CONFIG"] =
                "@xml/production_network_security_config"
            manifestPlaceholders["recreateAlarmsEnabled"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "Track & Graph")
            ndk.debugSymbolLevel = "SYMBOL_TABLE"
            manifestPlaceholders["ALLOW_CLEAR_TEXT"] = "false"
            manifestPlaceholders["NETWORK_SECURITY_CONFIG"] =
                "@xml/production_network_security_config"
            manifestPlaceholders["recreateAlarmsEnabled"] = "true"
        }
    }

    buildFeatures {
        dataBinding = true
        compose = true
        buildConfig = true
        resValues = true
    }

    // Dependency info blocks may present a security vulnerability in some cases and they are not
    // required for APK builds e.g. F-Droid builds. However they may contain useful info for
    // debugging issues on the Google Play Store.
    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Enables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = true
    }

    namespace = "com.samco.trackandgraph"
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":data"))
    implementation(project(":ui"))

    //Date and time
    implementation(libs.threetenabp)

    //Timber
    implementation(libs.timber)

    //Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    //AppCompat (enables dark theme on  API <= 29)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.core.ktx)

    //UI
    implementation(libs.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.viewbinding)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.material3)

    // Reorderable drag and drop
    implementation(libs.reorderable)
    
    // Markdown rendering
    implementation(libs.multiplatform.markdown.renderer)
    implementation(libs.multiplatform.markdown.renderer.m3)
    implementation(libs.multiplatform.markdown.renderer.coil3)
    
    // Coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Glance for widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.glance.appwidget.preview)
    implementation(libs.glance.preview)

    //Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    //Graph drawing
    implementation(libs.androidplot.core)

    //Work manager
    implementation(libs.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)

    //Backup and restore
    implementation(libs.room.runtime)

    //Fast immutable collection updates (used in functions screen)
    implementation(libs.kotlinx.collections.immutable)

    //Semantic versioning
    implementation(libs.semver)

    //Data Store
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    //Testing
    testImplementation(libs.threetenbp)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(project(":data")))

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)
}
