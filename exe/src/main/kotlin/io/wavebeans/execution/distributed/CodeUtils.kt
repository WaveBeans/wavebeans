package io.wavebeans.execution.distributed

import kotlinx.serialization.Serializable
import java.io.File
import java.util.jar.JarFile
import java.util.zip.CRC32

@Serializable
data class ClassDesc(
        val location: String,
        val classPath: String,
        val crc32: Long,
        val size: Long
)

fun startUpClasses(): List<ClassDesc> {
    return System.getProperty("java.class.path").split(":").asSequence()
            .map { File(it) }
            .filter { it.exists() }
            .map { codeLocation ->
                when {
                    codeLocation.isDirectory -> {
                        val l = mutableListOf<ClassDesc>()
                        classesInDirectory(codeLocation.absolutePath, "", codeLocation, l)
                        l.asSequence()
                    }
                    codeLocation.extension == "jar" -> {
                        JarFile(codeLocation).entries().asSequence()
                                .filter { it.name.endsWith(".class") }
                                .map { ClassDesc(codeLocation.absolutePath, it.name, it.crc, it.size) }
                    }
                    else -> throw UnsupportedOperationException("$codeLocation is not supported")
                }
            }.flatten().toList()
}

fun classesInDirectory(location: String, path: String, file: File, accumulator: MutableList<ClassDesc>) {
    if (file.isDirectory) {
        file.listFiles().forEach { classesInDirectory(location, "$path/${it.name}", it, accumulator) }
    } else {
        if (file.name.endsWith(".class")) {
            val crc32 = CRC32()
            val bytes = file.readBytes()
            crc32.update(bytes)
            accumulator += ClassDesc(location, path, crc32.value, bytes.size.toLong())
        }
    }
}