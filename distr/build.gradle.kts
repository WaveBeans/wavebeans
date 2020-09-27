import org.gradle.api.internal.plugins.DefaultTemplateBasedStartScriptGenerator

plugins {
    distribution
}

dependencies {
    project(":cli")
    project(":exe")
    project(":metrics:core")
    project(":metrics:prometheus")
}


val copyAdditionalFiles by tasks.registering {
    val filesToCopy = mapOf(
            "LICENSE" to "",
            "CHANGELOG.md" to "",
            "docs/user/" to "documentation",
            "README.md" to "documentation",
            "distr/resources/log-config.xml" to ""
    )
    val filesDir = file("$buildDir/extraFiles")
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
    val builtApps = file("$buildDir/builtCli")
    with(
            project(":cli").distributions.first().contents
    )
    destinationDir = builtApps
}

val makeCliScripts by tasks.registering(CreateStartScripts::class) {
    dependsOn(copyBuiltCli)
    outputDir = File("$buildDir/cliScripts")
    mainClassName = "io.wavebeans.cli.CliKt"
    applicationName = "wavebeans"
    classpath = fileTree(buildDir.absolutePath + "/builtCli/lib")
    (unixStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template = resources.text.fromFile("resources/unix.txt")
    (windowsStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template = resources.text.fromFile("resources/windows.txt")
}

val copyBuiltExe by tasks.registering(Copy::class) {
    dependsOn(":exe:installDist")
    val builtApps = file("$buildDir/builtExe")
    with(
            project(":exe").distributions.first().contents
    )
    destinationDir = builtApps
}

val makeExeScripts by tasks.registering(CreateStartScripts::class) {
    dependsOn(copyBuiltExe)
    outputDir = File("$buildDir/exeScripts")
    mainClassName = "io.wavebeans.execution.distributed.FacilitatorCliKt"
    applicationName = "wavebeans-facilitator"
    classpath = fileTree(buildDir.absolutePath + "/builtExe/lib")
    (unixStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template = resources.text.fromFile("resources/unix.txt")
    (windowsStartScriptGenerator as DefaultTemplateBasedStartScriptGenerator).template = resources.text.fromFile("resources/windows.txt")
}

val copyMetricsPrometheus by tasks.registering(Copy::class) {
    dependsOn(":metrics:prometheus:jar")
    val builtExtra = file("$buildDir/builtExtra/prometheus")
    from(project(":metrics:prometheus").buildDir.absolutePath + "/libs")
    from(
            project(":metrics:prometheus").configurations
                    .compileClasspath.get()
                    .copyRecursive { it.group?.indexOf("io.prometheus") ?: -1 >= 0 }
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
    val builtApps = file("$buildDir/builtApps")
    outputs.dirs(builtApps)
    doLast {
        val wavebeans = File(builtApps, "bin/wavebeans")
        File("$buildDir/cliScripts/wavebeans").copyTo(wavebeans, overwrite = true)
        wavebeans.setExecutable(true)
        File("$buildDir/cliScripts/wavebeans.bat").copyTo(File(builtApps, "bin/wavebeans.bat"), overwrite = true)

        val wavebeansFacilitator = File(builtApps, "bin/wavebeans-facilitator")
        File("$buildDir/exeScripts/wavebeans-facilitator").copyTo(wavebeansFacilitator, overwrite = true)
        wavebeansFacilitator.setExecutable(true)
        File("$buildDir/exeScripts/wavebeans-facilitator.bat").copyTo(File(builtApps, "bin/wavebeans-facilitator.bat"), overwrite = true)

        File("$buildDir/builtCli/lib").copyRecursively(File(builtApps, "lib"), overwrite = true)
        File("$buildDir/builtExe/lib").copyRecursively(File(builtApps, "lib"), overwrite = true)
        File("$buildDir/builtExtra").copyRecursively(File(builtApps, "extra"), overwrite = true)
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
