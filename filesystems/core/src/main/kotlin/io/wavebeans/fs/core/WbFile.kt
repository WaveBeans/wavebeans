package io.wavebeans.fs.core

/**
 * An abstraction thal allows to work with files whenever they are located.
 */
interface WbFile {
    /**
     * Check if the file exists.
     */
    fun exists(): Boolean

    /**
     * Deletes the file on the location.
     */
    fun delete(): Boolean

    fun createWbFileOutputStream(): WbFileOutputStream

    fun createWbFileInputStream(): WbFileInputStream
}