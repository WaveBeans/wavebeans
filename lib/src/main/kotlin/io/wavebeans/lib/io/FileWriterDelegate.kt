package io.wavebeans.lib.io

import io.wavebeans.fs.core.WbFile
import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.fs.core.WbFileOutputStream
import mu.KotlinLogging
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.URI

/**
 * Implements [WriterDelegate] to a file using [WbFileDriver] specified in schema of [uri].
 *
 * Writes temporary buffer to a temporary file created with [localFileFactory],  by default `file`,
 * once it is flushed or closed the file is copied over to desired location adding header and footer.
 *
 * The content is being buffered in the memory to avoid excessive IO. The size is defined by [bufferSize] and by
 * default is 512Kb.
 *
 * [uriGenerationStrategy] fetches the next path of the file when the delegate is created and then every time after
 * the content was flushed on the first written byte. The function has an argument of type [A], which, if is not null,
 * represents the argument associated with the signal in the [Managed] object when applicable.
 */
class FileWriterDelegate<A : Any>(
        private val uriGenerationStrategy: (A?) -> URI,
        private val bufferSize: Int = 512 * 1024,
        private val localFileFactory: WbFileDriver = WbFileDriver.instance("file"),
) : WriterDelegate<A> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    @Volatile
    private lateinit var uri: URI

    @Volatile
    private lateinit var tmpFile: WbFile

    @Volatile
    private lateinit var file: WbFileOutputStream

    @Volatile
    private lateinit var buffer: OutputStream

    @Volatile
    private var isReady: Boolean? = null
        private set

    @Volatile
    private var isEmpty = true

    @Volatile
    private var latestArgument: A? = null

    override fun write(b: Int) {
        check(isReady == true) { "The buffer is not ready to write into, initialize it first with initBuffer(). " }
        if (isEmpty) {
            log.trace { "[$this] Initializing buffer on write [latestArgument=$latestArgument, isEmpty=$isEmpty]" }
            doInitBuffer(latestArgument)
            latestArgument = null
            isEmpty = false
        }
        buffer.write(b)
    }

    override fun flush(argument: A?, headerFn: () -> ByteArray?, footerFn: () -> ByteArray?) {
        log.trace { "[$this] Flushing buffer [argument=$argument, isEmpty=$isEmpty]" }
        if (!isEmpty) {
            doFinalizeBuffer(headerFn, footerFn)
            latestArgument = argument
        }
    }

    override fun close(headerFn: () -> ByteArray?, footerFn: () -> ByteArray?) {
        log.trace { "[$this] Closing delegate" }
        if (!isEmpty) doFinalizeBuffer(headerFn, footerFn)
    }

    override fun initBuffer(argument: A?) {
        log.trace { "[$this] Initializing buffer [argument=$argument, isReady=$isReady]" }
        check(isReady == null || isReady == false) {
            "The delegate is not finalized to get initialized once again, finalize it first with finalizeBuffer()."
        }
        latestArgument = argument
        isReady = true
    }

    override fun finalizeBuffer(argument: A?, headerFn: () -> ByteArray?, footerFn: () -> ByteArray?) {
        log.trace { "[$this] Finalizing buffer [argument=$argument, isReady=$isReady]" }
        check(isReady != null && isReady == true) {
            "The delegate is not ready to get finalized, initialize it first with initBuffer()."
        }
        doFinalizeBuffer(headerFn, footerFn)
        isReady = false
    }

    private fun doInitBuffer(argument: A?) {
        uri = uriGenerationStrategy.invoke(argument)
        tmpFile = localFileFactory.createTemporaryWbFile("file-writer-output", ".tmp")
        file = tmpFile.createWbFileOutputStream()
        buffer = BufferedOutputStream(file, bufferSize)
    }

    private fun doFinalizeBuffer(headerFn: () -> ByteArray?, footerFn: () -> ByteArray?) {
        log.info { "[$this] Closing the buffer..." }
        try {
            buffer.close()
            log.debug { "[$this] Buffer closed" }
        } catch (e: IOException) {
            // ignore if buffer was already closed
            if ((e.message ?: "").contains("Closed"))
                log.warn(e) { "[$this] Buffer was already closed earlier" }
            else
                throw e
        }
        try {
            file.close()
            log.debug { "[$this] Temporary file $tmpFile closed" }
        } catch (e: IOException) {
            // ignore if buffer was already closed
            if ((e.message ?: "").contains("Closed"))
                log.warn(e) { "[$this] Temporary file $tmpFile was already closed earlier" }
            else
                throw e
        }

        if (tmpFile.exists()) {
            val resultFileStream = WbFileDriver.createFile(uri).createWbFileOutputStream()
            resultFileStream.use { f ->
                val header = headerFn()
                if (header != null) f.write(header)

                tmpFile.createWbFileInputStream().use { tmpF ->
                    val buf = ByteArray(bufferSize)
                    do {
                        val r = tmpF.read(buf)
                        if (r != -1) f.write(buf, 0, r)
                        log.trace { "[$this] Copied other $bufferSize bytes from $tmpFile to file with uri=$uri" }
                    } while (r != -1)
                }

                val footer = footerFn()
                if (footer != null) f.write(footer)
            }
            log.debug { "[$this] Temporary file $tmpFile copied over to file with uri=$uri" }
            tmpFile.delete()
            log.debug { "[$this] Temporary file $tmpFile deleted" }
        } else {
            log.warn { "[$this] Temporary file is not found. Seems everything is closed already." }
        }
        isEmpty = true
    }
}

fun <A : Any> plainFileWriterDelegate(uri: String): FileWriterDelegate<Unit> = FileWriterDelegate({ URI(uri) })

fun <A : Any> suffixedFileWriterDelegate(uri: String, suffix: (A?) -> String): FileWriterDelegate<A> = FileWriterDelegate({ argument ->
    val u = URI(uri)
    val f = File(u.path)
    val newFilePath = f.parent + File.separatorChar + f.nameWithoutExtension + suffix(argument) + "." + f.extension
    URI(u.scheme + "://" + newFilePath)
})