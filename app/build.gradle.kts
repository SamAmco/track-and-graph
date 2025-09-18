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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    id("shot")
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

apply(from = "gradle/lua-tasks.gradle.kts")

android {
    compileSdk = libs.versions.androidSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jdk.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jdk.get())
    }

    kotlin {
        jvmToolchain(libs.versions.jdk.get().toInt())

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            optIn.add("kotlin.RequiresOptIn")
        }
    }

    defaultConfig {
        applicationId = "com.samco.trackandgraph"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        //If the backup file is not backwards compatible after this update, upgrade the major version number!
        versionCode = 700005
        versionName = "7.0.5"
        testInstrumentationRunner = "com.samco.trackandgraph.screenshots.HiltTestRunner"
        // Default manifest placeholder for RecreateAlarms receiver
        manifestPlaceholders["recreateAlarmsEnabled"] = "true"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    //TODO you won't get syntax highlighting in the promo directory code
    // in android studio because of line if check will default to screenshots
    // but you can just comment it to say promo during development for now.

    // Dynamic testBuildType switching based on project properties
    testBuildType = if (project.hasProperty("usePromoTests")) "promo" else "screenshots"

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Debug Track & Graph")
            manifestPlaceholders["ALLOW_CLEAR_TEXT"] = "false"
            manifestPlaceholders["NETWORK_SECURITY_CONFIG"] = "@xml/debug_network_security_config"
            manifestPlaceholders["recreateAlarmsEnabled"] = "true"
        }
        create("debugMinify") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            applicationIdSuffix = ".debug.minify"
            resValue("string", "app_name", "Debug Minify Track & Graph")
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["ALLOW_CLEAR_TEXT"] = "false"
            manifestPlaceholders["NETWORK_SECURITY_CONFIG"] = "@xml/production_network_security_config"
            manifestPlaceholders["recreateAlarmsEnabled"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "Track & Graph")
            ndk.debugSymbolLevel = "SYMBOL_TABLE"
            manifestPlaceholders["ALLOW_CLEAR_TEXT"] = "false"
            manifestPlaceholders["NETWORK_SECURITY_CONFIG"] = "@xml/production_network_security_config"
            manifestPlaceholders["recreateAlarmsEnabled"] = "true"
        }
        create("screenshots") {
            initWith(getByName("release"))
            // Let “screenshots” resolve any debug-only deps
            matchingFallbacks += listOf("debug")
            // Flip the receiver OFF just for screenshots
            // because it runs before hilt has had a chance to inject
            // and crashes the tests
            manifestPlaceholders["recreateAlarmsEnabled"] = "false"
            // Disable minification to keep symbols/ids stable for tests
            isMinifyEnabled = false
            isShrinkResources = false
            // Use debug signing for testing (allows installation on emulator)
            signingConfig = signingConfigs.getByName("debug")
        }
        create("promo") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            // Flip the receiver OFF just for screenshots
            // because it runs before hilt has had a chance to inject
            // and crashes the tests
            manifestPlaceholders["recreateAlarmsEnabled"] = "false"
            // Disable minification to keep symbols/ids stable for tests
            isMinifyEnabled = false
            isShrinkResources = false
            // Use debug signing for testing (allows installation on emulator)
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        dataBinding = true
        compose = true
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

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":functions"))

    implementation(libs.androidx.legacy.support.v4)

    //Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    //AppCompat (enables dark theme on  API <= 29)
    implementation(libs.androidx.appcompat)
    implementation(libs.core.ktx)

    //Lua
    implementation(libs.luak.jvm)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.viewbinding)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.compose.runtime.livedata)
    // Material design stuff you might need at some point
    implementation(libs.compose.material3)

    // Reorderable drag and drop
    implementation(libs.reorderable)
    
    // Glance for widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.glance.appwidget.preview)
    implementation(libs.glance.preview)

    // Android KTX
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    //Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    //Lifecycle extensions
    implementation(libs.androidx.lifecycle.extensions)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    //Graph drawing
    implementation(libs.androidplot.core)

    //Work manager
    implementation(libs.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)

    // Moshi for JSON serialization
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    //Testing
    testImplementation(libs.threetenbp)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core.testing)

    // Instrumented tests
    androidTestImplementation(libs.shot.android)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.junit.ktx)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(testFixtures(project(":data")))
    kspAndroidTest(libs.hilt.compiler)
}
