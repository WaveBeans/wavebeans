package mux.lib.io

import mux.lib.Sample
import mux.lib.MuxStream

interface StreamInput : MuxStream<Sample, StreamInput>