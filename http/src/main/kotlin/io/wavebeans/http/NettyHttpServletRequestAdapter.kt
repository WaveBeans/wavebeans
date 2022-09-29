package io.wavebeans.http

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.Principal
import java.util.*
import javax.servlet.*
import javax.servlet.http.*
import kotlin.NoSuchElementException

fun <I : Sequence<T>, T> I.asEnumeration(): Enumeration<T> {
    val i = this.iterator()
    return object : Enumeration<T> {
        override fun hasMoreElements(): Boolean = i.hasNext()

        override fun nextElement(): T = i.next()
    }
}

class NettyHttpServletRequestAdapter(
    private val request: FullHttpRequest,
    private val contextPathValue: String = ""
) : HttpServletRequest {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val content by lazy {
        request.content()
    }

    private val inputStream by lazy {
        object : ServletInputStream() {


            override fun read(): Int {
                if (isFinished) throw NoSuchElementException("No more elements left")
                return content.readByte().toInt()
            }

            override fun isFinished(): Boolean = content.readableBytes() <= 0


            override fun isReady(): Boolean = content.readableBytes() > 0

            override fun setReadListener(readListener: ReadListener?) {
                TODO("Not yet implemented")
            }
        }
    }

    override fun getAttribute(name: String?): Any {
        TODO("Not yet implemented")
    }

    override fun getAttributeNames(): Enumeration<String> {
        TODO("Not yet implemented")
    }

    override fun getCharacterEncoding(): String {
        TODO("Not yet implemented")
    }

    override fun setCharacterEncoding(env: String?) {
        TODO("Not yet implemented")
    }

    override fun getContentLength(): Int {
        TODO("Not yet implemented")
    }

    override fun getContentLengthLong(): Long = request.headers()[HttpHeaderNames.CONTENT_LENGTH]?.toLong() ?: -1L

    override fun getContentType(): String {
        TODO("Not yet implemented")
    }

    override fun getInputStream(): ServletInputStream = inputStream

    override fun getParameter(name: String?): String {
        TODO("Not yet implemented")
    }

    override fun getParameterNames(): Enumeration<String> {
        TODO("Not yet implemented")
    }

    override fun getParameterValues(name: String?): Array<String> {
        TODO("Not yet implemented")
    }

    override fun getParameterMap(): MutableMap<String, Array<String>> {
        TODO("Not yet implemented")
    }

    override fun getProtocol(): String {
        TODO("Not yet implemented")
    }

    override fun getScheme(): String {
        TODO("Not yet implemented")
    }

    override fun getServerName(): String {
        TODO("Not yet implemented")
    }

    override fun getServerPort(): Int {
        TODO("Not yet implemented")
    }

    override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(inputStream))

    override fun getRemoteAddr(): String {
        TODO("Not yet implemented")
    }

    override fun getRemoteHost(): String {
        TODO("Not yet implemented")
    }

    override fun setAttribute(name: String?, o: Any?) {
        log.trace { "setAttribute(name=$name, o=$o)" }
    }

    override fun removeAttribute(name: String?) {
        TODO("Not yet implemented")
    }

    override fun getLocale(): Locale {
        TODO("Not yet implemented")
    }

    override fun getLocales(): Enumeration<Locale> {
        TODO("Not yet implemented")
    }

    override fun isSecure(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRequestDispatcher(path: String?): RequestDispatcher {
        TODO("Not yet implemented")
    }

    override fun getRealPath(path: String?): String {
        TODO("Not yet implemented")
    }

    override fun getRemotePort(): Int {
        TODO("Not yet implemented")
    }

    override fun getLocalName(): String {
        TODO("Not yet implemented")
    }

    override fun getLocalAddr(): String {
        TODO("Not yet implemented")
    }

    override fun getLocalPort(): Int {
        TODO("Not yet implemented")
    }

    override fun getServletContext(): ServletContext {
        TODO("Not yet implemented")
    }

    override fun startAsync(): AsyncContext {
        TODO("Not yet implemented")
    }

    override fun startAsync(servletRequest: ServletRequest?, servletResponse: ServletResponse?): AsyncContext {
        TODO("Not yet implemented")
    }

    override fun isAsyncStarted(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isAsyncSupported(): Boolean = false

    override fun getAsyncContext(): AsyncContext {
        TODO("Not yet implemented")
    }

    override fun getDispatcherType(): DispatcherType {
        TODO("Not yet implemented")
    }

    override fun getAuthType(): String {
        TODO("Not yet implemented")
    }

    override fun getCookies(): Array<Cookie> {
        return request.headers().getAll(HttpHeaderNames.COOKIE).map {
            val (name, value) = it.split("=", limit = 2)
            Cookie(name, value)
        }.toTypedArray()
    }

    override fun getDateHeader(name: String?): Long {
        TODO("Not yet implemented")
    }

    override fun getHeader(name: String?): String? = request.headers()[name]

    override fun getHeaders(name: String): Enumeration<String> {
        return request.headers().valueStringIterator(name).asSequence().asEnumeration()
    }

    override fun getHeaderNames(): Enumeration<String> {
        TODO("Not yet implemented")
    }

    override fun getIntHeader(name: String?): Int {
        TODO("Not yet implemented")
    }

    override fun getMethod(): String = request.method().name()

    override fun getPathInfo(): String {
        TODO("Not yet implemented")
    }

    override fun getPathTranslated(): String {
        TODO("Not yet implemented")
    }

    override fun getContextPath(): String {
        return contextPathValue
    }

    override fun getQueryString(): String = request.uri().dropWhile { it != '?' }.drop(1)

    override fun getRemoteUser(): String {
        TODO("Not yet implemented")
    }

    override fun isUserInRole(role: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getUserPrincipal(): Principal {
        TODO("Not yet implemented")
    }

    override fun getRequestedSessionId(): String {
        TODO("Not yet implemented")
    }

    override fun getRequestURI(): String {
        return request.uri().takeWhile { it != '?' }
    }

    override fun getRequestURL(): StringBuffer {
        TODO("Not yet implemented")
    }

    override fun getServletPath(): String {
        TODO("Not yet implemented")
    }

    override fun getSession(create: Boolean): HttpSession {
        TODO("Not yet implemented")
    }

    override fun getSession(): HttpSession {
        TODO("Not yet implemented")
    }

    override fun changeSessionId(): String {
        TODO("Not yet implemented")
    }

    override fun isRequestedSessionIdValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isRequestedSessionIdFromCookie(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isRequestedSessionIdFromURL(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isRequestedSessionIdFromUrl(): Boolean {
        TODO("Not yet implemented")
    }

    override fun authenticate(response: HttpServletResponse?): Boolean {
        TODO("Not yet implemented")
    }

    override fun login(username: String?, password: String?) {
        TODO("Not yet implemented")
    }

    override fun logout() {
        TODO("Not yet implemented")
    }

    override fun getParts(): MutableCollection<Part> {
        TODO("Not yet implemented")
    }

    override fun getPart(name: String?): Part {
        TODO("Not yet implemented")
    }

    override fun <T : HttpUpgradeHandler?> upgrade(handlerClass: Class<T>?): T {
        TODO("Not yet implemented")
    }

}