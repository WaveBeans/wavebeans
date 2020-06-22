package io.wavebeans.fs.core

import java.io.Closeable
import java.io.OutputStream

/**
 * WbFile output stream. Basically a copy of [java.io.OutputStream].
 */
abstract class WbFileOutputStream : OutputStream(), Closeable {
}