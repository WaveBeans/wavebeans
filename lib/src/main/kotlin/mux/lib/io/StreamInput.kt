package mux.lib.io

import mux.lib.BeanStream
import mux.lib.Sample
import mux.lib.SampleArray
import mux.lib.SourceBean

interface StreamInput : BeanStream<SampleArray, StreamInput>, SourceBean<SampleArray, StreamInput>