package mux.lib.io

import mux.lib.Sample
import mux.lib.TimeRangeProjectable
import mux.lib.stream.MuxStream
import mux.lib.stream.SampleStream

interface StreamInput : MuxStream<Sample>, TimeRangeProjectable<StreamInput>