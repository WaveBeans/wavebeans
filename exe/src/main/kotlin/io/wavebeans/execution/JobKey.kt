package io.wavebeans.execution

import java.util.*

typealias JobKey = UUID

fun newJobKey(): JobKey = UUID.randomUUID()
fun String.toJobKey(): JobKey = UUID.fromString(this)

