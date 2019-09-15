package mux.lib.io

import mux.lib.MuxStream
import mux.lib.Sample
import mux.lib.SourceMuxNode

interface StreamInput : MuxStream<Sample, StreamInput>, SourceMuxNode<Sample, StreamInput>