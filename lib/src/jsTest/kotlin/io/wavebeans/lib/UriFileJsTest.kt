package io.wavebeans.lib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UriFileJsTest {
    @Test
    fun uriParsesSchemeAndPathForAbsoluteAndRelativeCases() {
        val u1 = URI("file:///tmp/test.wav")
        assertEquals("file", u1.scheme)
        assertEquals("/tmp/test.wav", u1.path)

        val u2 = URI("s3://bucket/key/path.wav")
        assertEquals("s3", u2.scheme)
        assertEquals("bucket/key/path.wav", u2.path)

        val u3 = URI("/local/path.txt")
        assertTrue(u3.scheme.isEmpty())
        assertEquals("/local/path.txt", u3.path)
    }

    @Test
    fun uriAsStringReturnsOriginal() {
        val orig = "custom:abc"
        val u = URI(orig)
        assertEquals(orig, u.asString())
    }

    @Test
    fun fileComputesParentNameWithoutExtensionExtension() {
        val f1 = File("/a/b/c.txt")
        assertEquals("/a/b", f1.parent)
        assertEquals("c", f1.nameWithoutExtension)
        assertEquals("txt", f1.extension)

        val f2 = File("/a/.bashrc")
        assertEquals("/a", f2.parent)
        assertEquals(".bashrc", f2.nameWithoutExtension)
        assertEquals("", f2.extension)

        val f3 = File("name.with.many.dots.tar.gz")
        assertEquals("", f3.parent)
        assertEquals("name.with.many.dots.tar", f3.nameWithoutExtension)
        assertEquals("gz", f3.extension)
    }

    @Test
    fun fileUsesSlashAsSeparatorChar() {
        assertEquals('/', File.separatorChar)
    }
}
