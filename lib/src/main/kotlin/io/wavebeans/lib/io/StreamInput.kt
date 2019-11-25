package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SampleArray
import io.wavebeans.lib.SourceBean

interface StreamInput : BeanStream<SampleArray, StreamInput>, SourceBean<SampleArray, StreamInput>