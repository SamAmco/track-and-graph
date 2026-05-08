package com.samco.trackandgraph.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            compileSdk = libsVersion("androidSdk").toInt()

            defaultConfig {
                minSdk = libsVersion("minSdk").toInt()
            }

            compileOptions {
                sourceCompatibility = commonJavaVersion()
                targetCompatibility = commonJavaVersion()
            }
        }

        configureCommonKotlin()
    }
}
