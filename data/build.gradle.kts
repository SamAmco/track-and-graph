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
    id("org.jetbrains.kotlin.android")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdk = libs.versions.androidSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }

        sourceSets {
            getByName("androidTest").assets.srcDirs("$projectDir/schemas")
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("debugMinify") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
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
    
    namespace = "com.samco.trackandgraph.base"
}

dependencies {
    api(libs.threetenabp)

    api(libs.core.ktx)
    //Apparently we need material for the instrumented tests to build. idk why
    api(libs.material)

    //Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    //Database
    ksp(libs.room.compiler)
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    //CSV
    implementation(libs.commons.csv)

    //Timber
    api(libs.timber)

    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.junit.ktx)
    androidTestImplementation(libs.espresso.core)
}
