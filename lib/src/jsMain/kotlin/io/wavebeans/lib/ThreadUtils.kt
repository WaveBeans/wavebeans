package io.wavebeans.lib

actual fun yield() {
    // No-op for JS as it is single-threaded and has no native yield.
    // Cooperative yielding can be done via Coroutines, but this is a low-level expect.
}