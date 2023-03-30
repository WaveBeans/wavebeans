package io.wavebeans.fs.core

import io.wavebeans.lib.URI
import io.wavebeans.lib.io.InputStream
import io.wavebeans.lib.io.OutputStream


/**
 * An abstraction thal allows to work with files whenever they are located. Represent only the pointer to the file,
 * and if it is created the file itself may not exist, or be created.
 */
interface WbFile {

    /**
     * The [URI] of that file.
     */
    val uri: URI

    /**
     * Check if the file exists.
     *
     * @return true if file exists, otherwise false.
     */
    fun exists(): Boolean

    /**
     * Deletes the file on the location. Method doesn't fail if the file doesn't exist, though if any error happened
     * it just return false either.
     *
     * @return true if file was removed successfully, otherwise false.
     */
    fun delete(): Boolean

    /**
     * Creates the [WbFileOutputStream] to write the file content.
     * Always close the stream to make sure every single buffer is written and all underlying resources are released.
     *
     * @return instance of output stream.
     */
    fun createWbFileOutputStream(): OutputStream

    /**
     * Creates the [WbFileInputStream] to read the file content.
     * Always close the stream to make sure all underlying resources are released.
     *
     * @return instance of output stream.
     */
    fun createWbFileInputStream(): InputStream
}