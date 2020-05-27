package io.wavebeans.lib.io

import java.io.Closeable
import java.io.OutputStream

abstract class WriterDelegate : OutputStream(), Closeable {

    var headerFn: () -> ByteArray? = { null }
        internal set

    var footerFn: () -> ByteArray? = { null }
        internal set
}