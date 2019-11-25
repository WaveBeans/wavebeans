package io.wavebeans.cli.scope

import io.wavebeans.cli.Session
import io.wavebeans.cli.command.*
import java.util.concurrent.TimeUnit

private val tu = TimeUnit.NANOSECONDS

class AudioSubStreamScope(
        streamName: String,
        session: Session,
        parent: AudioStreamScope,
        selection: Selection
) : AudioStreamScope(
        session,
        streamName,
        parent.samples.rangeProjection(selection.start.time(tu), selection.end.time(tu), tu)
)