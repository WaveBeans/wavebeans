package io.wavebeans.lib

/**
 * Suspends the execution of the currently running thread to give other threads a chance to run.
 *
 * The thread that calls `yield` will be resumed in a later execution cycle, based on the scheduler's
 * conditions and available resources.
 *
 * Note that the exact behavior of this function might vary depending on the platform.
 */
expect fun yield()