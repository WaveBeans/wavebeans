package io.wavebeans.tests

import java.io.File

fun kotlincCmd(): String {
    val kotlinc = "kotlinc"
    val kotlinHome = System.getenv("KOTLIN_HOME")
        ?.takeIf { File("$it/$kotlinc").exists() }
        ?: System.getenv("PATH")
            .split(":")
            .firstOrNull { File("$it/$kotlinc").exists() }
        ?: throw IllegalStateException(
            "$kotlinc is not located, make sure it is available via either" +
                    " PATH or KOTLIN_HOME environment variable"
        )

    return "$kotlinHome/$kotlinc"
}

fun compileCode(codeFiles: Map<String, String>): File {
    val jarFile = File(createTempDir("wavebeans-code").also { it.deleteOnExit() }, "code.jar")
    val tempDir = createTempDir("wavebeans-test-code").also { it.deleteOnExit() }
    val scriptFiles = codeFiles.entries.map { (name, content) ->
        File(tempDir, name).also { it.writeText(content) }
    }

    val compileCall = CommandRunner(
        kotlincCmd(),
        "-d", jarFile.absolutePath, *scriptFiles.map { it.absolutePath }.toTypedArray(),
        "-cp", System.getProperty("java.class.path"),
        "-jvm-target", "11"
    ).run()

    if (compileCall.exitCode != 0) {
        throw IllegalStateException("Can't compile the code. \n${String(compileCall.output)}")
    }

    return jarFile
}

fun javaCmd(): String {
    val java = "java"
    val javaHome = System.getenv("JAVA_HOME")
        ?.takeIf { File("$it/$java").exists() }
        ?: System.getenv("PATH")
            .split(":")
            .firstOrNull { File("$it/$java").exists() }
        ?: throw IllegalStateException(
            "$java is not located, make sure it is available via either" +
                    " PATH or JAVA_HOME environment variable"
        )

    return "$javaHome/$java"
}
