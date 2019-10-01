package mux.lib.io

import mux.lib.BeanStream
import mux.lib.Sample
import mux.lib.SourceBean

interface StreamInput : BeanStream<Sample, StreamInput>, SourceBean<Sample, StreamInput>