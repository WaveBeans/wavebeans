package io.wavebeans.http

import io.netty.buffer.ByteBuf
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.handler.codec.http.*
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.PrintWriter
import java.util.*
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

class NettyHttpServletResponseAdapter(val response: FullHttpResponse) : HttpServletResponse, Closeable {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    val byteBuffer: ByteBuf by lazy {  UnpooledByteBufAllocator(false).heapBuffer() }

    private val outputStreamImpl by lazy {
        object : ServletOutputStream() {

            override fun write(b: Int) {
                byteBuffer.writeByte(b)
            }

            override fun isReady(): Boolean = true

            override fun setWriteListener(writeListener: WriteListener?) {
                TODO("Not yet implemented")
            }
        }

    }

    private val printWriter by lazy { PrintWriter(outputStreamImpl) }

    override fun getCharacterEncoding(): String = response.headers()[HttpHeaderNames.CONTENT_ENCODING]

    override fun getContentType(): String = response.headers()[HttpHeaderNames.CONTENT_TYPE]

    override fun getOutputStream(): ServletOutputStream = outputStreamImpl

    override fun getWriter(): PrintWriter = printWriter

    override fun setCharacterEncoding(charset: String?) {
        TODO("Not yet implemented")
    }

    override fun setContentLength(len: Int) {
        log.trace { "setContentLength(len=$len)" }
        response.headers()[HttpHeaderNames.CONTENT_LENGTH] = len
    }

    override fun setContentLengthLong(len: Long) {
        log.trace { "setContentLengthLong(len=$len)" }
        response.headers()[HttpHeaderNames.CONTENT_LENGTH] = len
    }

    override fun setContentType(type: String) {
        log.trace { "setContentType(type=$type)" }
        response.headers()[HttpHeaderNames.CONTENT_TYPE] = type
    }

    override fun setBufferSize(size: Int) {
        TODO("Not yet implemented")
    }

    override fun getBufferSize(): Int {
        TODO("Not yet implemented")
    }

    override fun flushBuffer() {
        printWriter.flush()
    }

    override fun resetBuffer() {
        TODO("Not yet implemented")
    }

    override fun isCommitted(): Boolean {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun setLocale(loc: Locale?) {
        TODO("Not yet implemented")
    }

    override fun getLocale(): Locale {
        TODO("Not yet implemented")
    }

    override fun addCookie(cookie: Cookie?) {
        TODO("Not yet implemented")
    }

    override fun containsHeader(name: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun encodeURL(url: String?): String {
        TODO("Not yet implemented")
    }

    override fun encodeRedirectURL(url: String?): String {
        TODO("Not yet implemented")
    }

    override fun encodeUrl(url: String?): String {
        TODO("Not yet implemented")
    }

    override fun encodeRedirectUrl(url: String?): String {
        TODO("Not yet implemented")
    }

    override fun sendError(sc: Int, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun sendError(sc: Int) {
        TODO("Not yet implemented")
    }

    override fun sendRedirect(location: String?) {
        TODO("Not yet implemented")
    }

    override fun setDateHeader(name: String?, date: Long) {
        TODO("Not yet implemented")
    }

    override fun addDateHeader(name: String?, date: Long) {
        TODO("Not yet implemented")
    }

    override fun setHeader(name: String, value: String?) {
        log.trace { "setHeader(name=$name, value=$value)" }
        response.headers()[name] = value
    }

    override fun addHeader(name: String?, value: String?) {
        TODO("Not yet implemented")
    }

    override fun setIntHeader(name: String?, value: Int) {
        TODO("Not yet implemented")
    }

    override fun addIntHeader(name: String?, value: Int) {
        TODO("Not yet implemented")
    }

    override fun setStatus(sc: Int) {
        log.trace { "setStatus(sc=$sc)" }
        response.status = HttpResponseStatus.valueOf(sc)
    }

    override fun setStatus(sc: Int, sm: String?) {
        TODO("Not yet implemented")
    }

    override fun getStatus(): Int = response.status().code()

    override fun getHeader(name: String?): String? = response.headers()[name]

    override fun getHeaders(name: String?): MutableCollection<String> {
        TODO("Not yet implemented")
    }

    override fun getHeaderNames(): MutableCollection<String> {
        TODO("Not yet implemented")
    }

    override fun close() {
        outputStreamImpl.close()
    }
}