package io.wavebeans.execution.distributed

import java.net.URL
import java.net.URLClassLoader

class FacilitatorClassLoader(parent: ClassLoader) : URLClassLoader(emptyArray(), parent) {
    operator fun plusAssign(url: URL) {
        addURL(url)
    }
}