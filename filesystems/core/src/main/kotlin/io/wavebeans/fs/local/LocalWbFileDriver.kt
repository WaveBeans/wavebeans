package io.wavebeans.fs.local

import io.wavebeans.fs.core.WbFile
import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.lib.URI
import java.io.File

object LocalWbFileDriver : WbFileDriver {

    override fun createTemporaryWbFile(prefix: String, suffix: String, parent: WbFile?): WbFile {
        return if (parent == null)
            LocalWbFile(File.createTempFile(prefix, suffix))
        else if (parent is LocalWbFile)
            LocalWbFile(File.createTempFile(prefix, suffix, parent.file))
        else
            throw IllegalArgumentException("Can't create a file with parent of different type: $parent")
    }


    override fun createWbFile(uri: URI): WbFile = LocalWbFile(File(uri.path))

}