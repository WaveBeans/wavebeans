package io.wavebeans.fs.core

import java.io.Closeable
import java.io.InputStream

/**
 * WbFile input stream. Basically a copy of [java.io.InputStream].
 */
abstract class WbFileInputStream : InputStream(), Closeable {

}