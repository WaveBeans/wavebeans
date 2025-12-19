package io.wavebeans.lib

actual class URI actual constructor(uri: String) {
    private val original: String = uri

    actual val scheme: String
        get() {
            // simple RFC3986-like scheme parse: ^[a-zA-Z][a-zA-Z0-9+.-]*:
            val i = original.indexOf(':')
            if (i <= 0) return ""
            // ensure ':' appears before any '/' which would indicate a path without scheme like /c:/path on windows (not used in JS)
            val slash = original.indexOf('/')
            if (slash in 0 until i) return ""
            val s = original.substring(0, i)
            val valid = s.isNotEmpty() && s[0].isLetter() && s.all { it.isLetterOrDigit() || it == '+' || it == '.' || it == '-' }
            return if (valid) s else ""
        }
    actual val path: String
        get() {
            val sc = scheme
            if (sc.isEmpty()) return original
            val schemeSep = "$sc:"
            // if authority present (://), strip it, keep the rest as path
            val withAuth = "$schemeSep//"
            return if (original.startsWith(withAuth)) {
                original.substring(withAuth.length)
            } else {
                original.substring(schemeSep.length)
            }
        }

    actual fun asString(): String {
        return original
    }
}

actual class File actual constructor(path: String) {
    private val original: String = path
    actual companion object {
        actual val separatorChar: Char
            get() = '/'
    }

    actual val parent: String
        get() {
            val idx = original.lastIndexOf(separatorChar)
            return if (idx <= 0) "" else original.substring(0, idx)
        }
    actual val nameWithoutExtension: String
        get() {
            val name = run {
                val idx = original.lastIndexOf(separatorChar)
                if (idx >= 0) original.substring(idx + 1) else original
            }
            val dot = name.lastIndexOf('.')
            if (dot <= 0) return name // no dot or leading dot (hidden file)
            return name.substring(0, dot)
        }
    actual val extension: String
        get() {
            val name = run {
                val idx = original.lastIndexOf(separatorChar)
                if (idx >= 0) original.substring(idx + 1) else original
            }
            val dot = name.lastIndexOf('.')
            if (dot <= 0 || dot == name.length - 1) return ""
            return name.substring(dot + 1)
        }

}