package io.wavebeans.fs.local

import io.wavebeans.fs.core.*
import io.wavebeans.lib.URI
import io.wavebeans.lib.io.InputStream
import io.wavebeans.lib.io.OutputStream
import java.io.File

class LocalWbFile(val file: File) : WbFile {

    override val uri: URI
        get() = URI(file.toURI().toString())

    override fun exists(): Boolean = file.exists()

    override fun delete(): Boolean = file.delete()

    override fun createWbFileOutputStream(): OutputStream = LocalWbFileOutputStream(this)

    override fun createWbFileInputStream(): InputStream = LocalWbFileInputStream(this)

    override fun toString(): String {
        return "LocalWbFile(uri=$uri)"
    }
}

