package com.samco.trackandgraph.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = libsVersion("androidSdk").toInt()

            defaultConfig {
                minSdk = libsVersion("minSdk").toInt()
                targetSdk = libsVersion("targetSdk").toInt()
            }

            compileOptions {
                sourceCompatibility = commonJavaVersion()
                targetCompatibility = commonJavaVersion()
            }
        }

        configureCommonKotlin()
    }
}
