package io.wavebeans.lib.io

import io.wavebeans.fs.core.WbFile
import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.lib.URI
import io.wavebeans.lib.io.TestWbFileDriver.Companion.driver
import io.wavebeans.lib.table.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlin.random.Random

data class TestFile(val url: String) {

    fun readLines(): List<String>? {
        return readText()?.trim()?.lines()
    }

    fun readText(): String? {
        return driver.fs[url]?.decodeToString()
    }
}

class TestWbFileDriver(val fs: MutableMap<String, ByteArray>) : WbFileDriver {
    override fun createTemporaryWbFile(prefix: String, suffix: String, parent: WbFile?): WbFile {
        return TestWbFile(fs, URI("test://$prefix${Random.nextLong().toString(36)}s$suffix"))
    }

    override fun createWbFile(uri: URI): WbFile {
        return TestWbFile(fs, uri)
    }

    companion object {
        val driver = TestWbFileDriver(ConcurrentHashMap())

        fun register() {
            WbFileDriver.registerDriver("test", driver)
        }

        fun unregister() {
            WbFileDriver.unregisterDriver("test")
        }

        fun createTempFile(): TestFile {
            val name = Random.nextLong().absoluteValue.toString(36) + ".tmp"
            return TestFile("test:///$name")
        }

        fun listFiles(dir: String): List<TestFile> {
            return driver.fs.keys
                    .filter { it.startsWith("test:///${dir.trimEnd('/')}/") }
                    .map { TestFile(it) }
        }

    }
}