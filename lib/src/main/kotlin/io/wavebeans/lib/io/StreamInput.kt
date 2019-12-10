package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.SourceBean

interface StreamInput : BeanStream<Sample>, SourceBean<Sample>
