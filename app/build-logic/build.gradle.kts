plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "tng.android.application"
            implementationClass = "com.samco.trackandgraph.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "tng.android.library"
            implementationClass = "com.samco.trackandgraph.buildlogic.AndroidLibraryConventionPlugin"
        }
    }
}
