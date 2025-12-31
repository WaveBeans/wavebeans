package io.wavebeans.lib

import io.kotest.core.config.AbstractProjectConfig
import kotlin.time.Duration.Companion.seconds

class KotestConfig : AbstractProjectConfig() {
    override val timeout = 10.seconds
}