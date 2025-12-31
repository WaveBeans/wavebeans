package io.wavebeans.lib.io

/**
 * Implements the actual writing (on the disk, into the memory, etc) of the "buffer". Mainly used within [AbstractWriter].
 *
 * The delegate is a stateful object, by default non-thread-safe.
 *
 * It is expected to follow the next steps:
 * 1. When it is created the new temporary "buffer" is created, where all content is being saved while streaming.
 *    The data is written by calling any `write` function [WriterDelegate.write].
 * 2. When the [WriterDelegate.flush] is being called, the buffer should be finalized and all the content of the
 *   temporary "buffer" is stored in final "buffer" prefixed by the header fetched by [WriterDelegate.headerFn] and
 *   suffixed by footer fetched by [WriterDelegate.footerFn].
 * 3. When the [WriterDelegate.close] is called the delegate finishes its work and finalizes the "buffer" similar to
 *    [WriterDelegate.flush]. After this method is called no more calls to delegate is expected.
 *
 * @param A type of the flush arguments.
 */
interface WriterDelegate<A : Any> {

    /**
     * Writes the [Byte] represented as [Int] to the current buffer.
     *
     * @param b byte to write into a buffer.
     */
    fun write(b: Int)

    /**
     * Writes the [ByteArray] into a current "buffer".
     *
     * @param buf the buffer to get bytes from.
     * @param start the index to get the bytes starting from, by default 0. Must be >=0.
     * @param end the index to the get the bytes up to, exclusive, by default [buf].size.
     */
    fun write(buf: ByteArray, start: Int = 0, end: Int = buf.size) {
        buf.asSequence()
                .drop(start)
                .take(end - start)
                .map { it.toInt() and 0xFF }
                .forEach(::write)
    }

    /**
     * Finalizes the current "buffer" and starts a new one. I.e. in case of writing to file expected to create a new file
     * for the next content.
     *
     * @param argument the argument to  bypass, usually used for new name generation, follow the documentation
     *                 of the specific implementation.
     * @param headerFn  function to use to write some bytes before the actual content. If there is no header to be
     *                  written returns `null`. Default implementation returns `null`.
     * @param footerFn  function to use to write some bytes after the actual content. If there is no footer to be
     *                  written returns `null`. Default implementation returns `null`.
     */
    fun flush(argument: A?, headerFn: () -> ByteArray? = { null }, footerFn: () -> ByteArray? = { null })

    /**
     * Manually initiates the new buffer if it isn't initialized yet. Depending on implementation may or may not support multiple calls.
     *
     * @param argument the argument to  bypass, usually used for new name generation, follow the documentation
     *                 of the specific implementation.
     */
    fun initBuffer(argument: A?)

    /**
     * Manually finalizes the new buffer if it isn't initialized yet. Depending on implementation may or may not support multiple calls.
     *
     * @param argument the argument to  bypass, usually used for new name generation, follow the documentation
     *                 of the specific implementation.
     * @param headerFn  function to use to write some bytes before the actual content. If there is no header to be
     *                  written returns `null`. Default implementation returns `null`.
     * @param footerFn  function to use to write some bytes after the actual content. If there is no footer to be
     *                  written returns `null`. Default implementation returns `null`.
     */
    fun finalizeBuffer(argument: A?, headerFn: () -> ByteArray? = { null }, footerFn: () -> ByteArray? = { null })

    /**
     * Closes the writer and finalizes the current "buffer". When called, it is not expected to call any other method more.
     *
     * @param headerFn  function to use to write some bytes before the actual content. If there is no header to be
     *                  written returns `null`. Default implementation returns `null`.
     * @param footerFn  function to use to write some bytes after the actual content. If there is no footer to be
     *                  written returns `null`. Default implementation returns `null`.
     */
    fun close(headerFn: () -> ByteArray? = { null }, footerFn: () -> ByteArray? = { null })
}