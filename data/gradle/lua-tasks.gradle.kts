// Function to add a warning comment before copying
fun prepareGeneratedFile(source: File, target: File) {
    val content = source.readText(Charsets.UTF_8)
    val warningComment = "-- GENERATED FILE: DO NOT EDIT MANUALLY\n\n"
    target.writeText(warningComment + content)
}

// Function to copy all Lua api files to a specified destination directory
fun copyLuaApiToDestination(destinationDir: File) {
    destinationDir.mkdirs()
    rootProject.file("lua/src/tng").listFiles()?.forEach { file ->
        if (file.isFile) {
            val destinationFile = File(destinationDir, file.name)
            prepareGeneratedFile(file, destinationFile)
        }
    }
}

// Task to copy all Lua files to the generated assets directory
tasks.register("copyLuaToAssets") {
    doLast {
        // First clean up the destination directory
        val destinationDir = file("src/main/assets/generated/lua-api/")
        destinationDir.deleteRecursively()
        copyLuaApiToDestination(destinationDir)
    }
}

// Ensure the assets copy runs before every build
tasks.matching { it.name.contains("assemble", ignoreCase = true) || it.name.contains("bundle", ignoreCase = true) }.configureEach {
    dependsOn("copyLuaToAssets")
}

// TEST TASKS

// Task to copy Lua API scripts
tasks.register("copyLuaScriptsForTests") {
    doLast {
        // First clean up the destination directory
        file("src/test/resources/generated/").deleteRecursively()

        copy {
            from("${rootProject.projectDir}/lua/src/community")
            into("${projectDir}/src/test/resources/generated/lua-community")
            include("**/*.lua")
        }

        copyLuaApiToDestination(file("src/test/resources/generated/lua-api/"))

        val destinationDir = file("src/test/resources/generated/lua-test/")
        destinationDir.mkdirs()
        rootProject.file("lua/src/test").listFiles()?.forEach { file ->
            if (file.isFile) {
                val destinationFile = File(destinationDir, file.name)
                prepareGeneratedFile(file, destinationFile)
            }
        }
    }
}

// Ensure all Lua scripts are copied before any unit test task
tasks.matching { it.name.startsWith("test") }.configureEach {
    dependsOn("copyLuaScriptsForTests")
}

// CLEAN UP TASKS

tasks.register("cleanUpLuaFiles") {
    doLast {
        file("src/main/assets/generated/").deleteRecursively()
        file("src/test/resources/generated/").deleteRecursively()
    }
}

tasks.named("clean").configure {
    dependsOn("cleanUpLuaFiles")
}
