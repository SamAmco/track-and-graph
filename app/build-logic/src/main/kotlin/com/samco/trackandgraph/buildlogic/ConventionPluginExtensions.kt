package com.samco.trackandgraph.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureCommonKotlin() {
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(libsVersion("buildJdk").toInt())

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
}

internal fun Project.commonJavaVersion(): JavaVersion =
    JavaVersion.toVersion(libsVersion("jdk"))

internal fun Project.libsVersion(name: String): String =
    the<VersionCatalogsExtension>()
        .named("libs")
        .findVersion(name)
        .get()
        .requiredVersion
