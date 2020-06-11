package io.wavebeans.fs.core

import java.io.File

class LocalWbFile(val file: File) : WbFile {
    override fun exists(): Boolean = file.exists()

    override fun delete(): Boolean = file.delete()

    override fun createWbFileOutputStream(): WbFileOutputStream = LocalWbFileOutputStream(this)

    override fun createWbFileInputStream(): WbFileInputStream = LocalWbFileInputStream(this)
}

