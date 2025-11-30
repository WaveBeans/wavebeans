import org.gradle.api.internal.plugins.DefaultTemplateBasedStartScriptGenerator

plugins {
    distribution
}

dependencies {
    project(":cli")
    project(":exe")
}

fun Project.buildDirectoryPath(): String = layout.buildDirectory.asFile.get().absolutePath

val copyAdditionalFiles by tasks.registering {
    val filesToCopy = mapOf(
        "LICENSE" to "",
        "CHANGELOG.md" to "",
        "docs/user/" to "documentation",
        "README.md" to "documentation",
        "distr/resources/log-config.xml" to ""
    )
    val filesDir = file("${buildDirectoryPath()}/extraFiles")
    outputs.dir(filesDir)
    doLast {
        filesDir.mkdirs()
        filesToCopy.forEach { (fileName, path) ->
            val target = File("${filesDir.absolutePath}/$path")
            target.mkdirs()
            val source = File(rootDir.absoluteFile, fileName)
            if (source.isDirectory) {
                source.copyRecursively(target, overwrite = true)
            } else {
                source.copyTo(File(target, File(fileName).name), overwrite = true)
            }
        }
    }
}

val copyBuiltCli by tasks.registering(Copy::class) {
    dependsOn(":cli:installDist")
    val builtApps = file("${buildDirectoryPath()}/builtCli")
    with(
        project(":cli").distributions.first().contents
    )
    destinationDir = builtApps
}

val makeCliScripts by tasks.registering(CreateStartScripts::class) {
    dependsOn(copyBuiltCli)
    outputDir = File("${buildDirectoryPath()}/cliScripts")
    mainClass.set("io.wavebeans.cli.CliKt")
    applicationName = "wavebeans"
    classpath = fileTree(buildDirectoryPath() + "/builtCli/lib")
    (unixStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template =
        resources.text.fromFile("resources/unix.txt")
    (windowsStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template =
        resources.text.fromFile("resources/windows.txt")
}

val copyBuiltExe by tasks.registering(Copy::class) {
    dependsOn(":exe:installDist")
    val builtApps = file("${buildDirectoryPath()}/builtExe")
    with(
        project(":exe").distributions.first().contents
    )
    destinationDir = builtApps
}

val makeExeScripts by tasks.registering(CreateStartScripts::class) {
    dependsOn(copyBuiltExe)
    outputDir = File("${buildDirectoryPath()}/exeScripts")
    mainClass.set("io.wavebeans.execution.distributed.FacilitatorCliKt")
    applicationName = "wavebeans-facilitator"
    classpath = fileTree(buildDirectoryPath() + "/builtExe/lib")
    (unixStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template =
        resources.text.fromFile("resources/unix.txt")
    (windowsStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template =
        resources.text.fromFile("resources/windows.txt")
}


val copyMetricsPrometheus by tasks.registering(Copy::class) {
    dependsOn(":metrics-prometheus:assemble")
    val builtExtra = file("${buildDirectoryPath()}/builtExtra/prometheus")
    from(project(":metrics-prometheus").layout.buildDirectory.asFile.get().absolutePath + "/libs")
    from(
        project(":metrics-prometheus").configurations
            .runtimeClasspath.get()
            .resolvedConfiguration
            .resolvedArtifacts
            .filter { it.moduleVersion.id.group.indexOf("io.prometheus") >= 0 }
            .map { it.file }
    )
    destinationDir = builtExtra

    doLast {
        val cpFiles = builtExtra.listFiles()
        File(builtExtra, "classpath-unix.txt").writeText(cpFiles?.joinToString(":") { it.name } ?: "")
        File(builtExtra, "classpath-win.txt").writeText(cpFiles?.joinToString(";") { it.name } ?: "")
    }
}

val copyAll by tasks.registering {
    dependsOn(copyBuiltCli, makeCliScripts, copyBuiltExe, makeExeScripts, copyMetricsPrometheus)
    val builtApps = file("${buildDirectoryPath()}/builtApps")
    outputs.dirs(builtApps)
    doLast {
        val wavebeans = File(builtApps, "bin/wavebeans")
        File("${buildDirectoryPath()}/cliScripts/wavebeans").copyTo(wavebeans, overwrite = true)
        wavebeans.setExecutable(true)
        File("${buildDirectoryPath()}/cliScripts/wavebeans.bat").copyTo(
            File(builtApps, "bin/wavebeans.bat"),
            overwrite = true
        )

        val wavebeansFacilitator = File(builtApps, "bin/wavebeans-facilitator")
        File("${buildDirectoryPath()}/exeScripts/wavebeans-facilitator").copyTo(wavebeansFacilitator, overwrite = true)
        wavebeansFacilitator.setExecutable(true)
        File("${buildDirectoryPath()}/exeScripts/wavebeans-facilitator.bat").copyTo(
            File(
                builtApps,
                "bin/wavebeans-facilitator.bat"
            ), overwrite = true
        )

        File("${buildDirectoryPath()}/builtCli/lib").copyRecursively(File(builtApps, "lib"), overwrite = true)
        File("${buildDirectoryPath()}/builtExe/lib").copyRecursively(File(builtApps, "lib"), overwrite = true)
        File("${buildDirectoryPath()}/builtExtra").copyRecursively(File(builtApps, "extra"), overwrite = true)
    }
}

distributions {
    main {
        distributionBaseName.set("wavebeans")
        contents {
            from(copyAdditionalFiles)
            from(copyAll)
        }
    }
}
