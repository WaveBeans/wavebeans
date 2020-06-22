package io.wavebeans.fs.local

import io.wavebeans.fs.core.*
import java.io.File
import java.net.URI

class LocalWbFile(val file: File) : WbFile {

    override val uri: URI
        get() = file.toURI()

    override fun exists(): Boolean = file.exists()

    override fun delete(): Boolean = file.delete()

    override fun createWbFileOutputStream(): WbFileOutputStream = LocalWbFileOutputStream(this)

    override fun createWbFileInputStream(): WbFileInputStream = LocalWbFileInputStream(this)

    override fun toString(): String {
        return "LocalWbFile(uri=$uri)"
    }
}

