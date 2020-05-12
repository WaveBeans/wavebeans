package io.wavebeans.execution

import java.util.concurrent.ThreadFactory

class NamedThreadFactory(val name: String) : ThreadFactory {
    private var c = 0
    override fun newThread(r: Runnable): Thread {
        return Thread(ThreadGroup(name), r, "$name-${c++}")
    }
}