Version 0.3.0 on 2021-01-02
------

* **[Breaking changes]** SampleArray renamed to [SampleVector](/docs/user/api/readme.md#samplevector) as well as it has wider API and usage overall.
* **[Breaking changes]** Now based on Kotlin 1.4. Previous Kotlin versions are not guaranteed to work.
    * Kotlin 1.4.21
* [ [#31](https://github.com/WaveBeans/wavebeans/issues/31) ] [Flatten](/docs/user/api/operations/flatten.md) operation.
* [ [#86](https://github.com/WaveBeans/wavebeans/issues/86) ] [Managing](/docs/user/api/readme.md#managed-type) and controlling [WAV](/docs/user/api/outputs/wav-output.md#controlling-output) and [CSV](/docs/user/api/outputs/csv-outputs.md#controlling-output) outputs
* [Output as a function](/docs/user/api/outputs/output-as-a-function.md)
* [Inverse FFT](/docs/user/api/operations/fft-operation.md#inverse-fft) implementation
* [Window functions](/docs/user/api/operations/fft-operation.md#window-functions) are available as separate convenience functions 
* [Resample](/docs/user/api/operations/resample-operation.md) operation
    * An automatic resampling for [wav-file inputs](/docs/user/api/inputs/wav-file.md#resampling)
* Monitoring: metrics system, internal collectors and Prometheus exporter. More in [docs](/docs/user/ops/monitoring.md). 
* Improvements on [SampleVector](/docs/user/api/readme.md#samplevector) type, i.e. arithmetic operation with scalars, an operation on two non-nullable vector gets non-nullable result. 

Version 0.2.0 on 2020-08-12
------

* [ [#82](https://github.com/WaveBeans/wavebeans/issues/82) ] [Concatenation](/docs/user/api/operations/concatenation-operation.md) operation
* File systems support:
    * File System abstraction for a better support of different location types of operations on files.
    * DropBox implementation as File System, follow [usage guide](/docs/user/api/file-systems.md#dropbox-file-system).
* HTTP Service improvements: 
    * [ [#62](https://github.com/WaveBeans/wavebeans/issues/62) ] HTTP API improvements. Audio and Table service no longer require some parameters (sampleRate, sourceType) which can be inferred from the table itself.
    * HTTP service now may stream data out of the table with the help of [Audio Service](/docs/user/http/readme.md#audio-service).
    * HTTP Service is CORS-enabled
* Table Output improvements:
    * If the table is based on finite stream the audio streaming to support the end of the stream as well.
    * Specific [Table API for Samples and SampleArrays](/docs/user/api/outputs/table-output.md#sample-type)
    * Table implementation can now be [provided as a parameter](/docs/user/api/outputs/table-output.md#custom-table-implementation).
    * Introduced remote table driver implementation and leveraging it in HTTP service, so now HTTP service may provide access to tables while running in distributed mode. More details in [documentation](/docs/user/http/readme.md#distributed-mode)
    * Better contextual documentation for Table Output.
* Other: 
    * Introduced [SampleArray](/docs/user/api/readme.md#samplearray) type for performance optimization of certain use cases.
    * [ [#59](https://github.com/WaveBeans/wavebeans/issues/59) ] Switched internal communication to gRPC.
    * [Bugfix] 24bit wave file storing and fetching hasn't been working properly.
    * [Bugfix] The table output is not correctly measuring time markers for complex objects 
    * [Internal] Wav Writer slighly refactored to be more reusable in different parts of the system.

Version 0.1.0 on 2020-05-18
------

Entering the **new era** with actual distributed processing. Now it's still Work In Progress though have some abilities to play around with.

* The first version of evaluation in Distributed mode. Follow the [docs](/docs/user/exe/readme.md#distributed-mode).
* Proper multi-threaded mode that doesn't require serialization in place. Follow the [docs](/docs/user/exe/readme.md#multi-threaded-mode).
* [ [#52](https://github.com/WaveBeans/wavebeans/issues/52) ] Custom class that requires measurement needs to implement `Measured` interface. See [updated section of documentation](/docs/user/api/operations/projection-operation.md#working-with-different-types)
* A first version of [Developer zone](/docs/dev/) -- a bunch of documents explaining ideas and architecture behind WaveBeans.

Version 0.0.3 on 2020-04-03
------

* Execution: **Breaking changes** `Overseer.eval()` now returns different type of result. Follow [docs](/docs/user/exe/readme.md)
* CLI: Using Kotlin Scripting as main engine. No need to install Kotlin SDK anymore
* Support of different [window functions](https://en.wikipedia.org/wiki/Window_function):
  * [Documentation](/docs/user/api/operations/map-window-function.md)
  * Implementation for [Sample](/docs/user/api/operations/map-window-function.md#stream-of-sample-type) type:
    * [rectangular](https://en.wikipedia.org/wiki/Window_function#Rectangular_window)
    * [triangular](https://en.wikipedia.org/wiki/Window_function#Triangular_window)
    * [blackman](https://en.wikipedia.org/wiki/Window_function#Blackman_window)
    * [hamming](https://en.wikipedia.org/wiki/Window_function#Hann_and_Hamming_windows)
  * Generic implementation for [any type](/docs/user/api/operations/map-window-function.md#stream-of-any-type)
* Better support for types using through HTTP/Table service, distributed execution and measuring for different purposes:
    * `FftSample`
    * `Window<T>`
    * `List<Double>`
* Detecting valid classloader depending on the environment you're running in (i.e. regular app, Jupyter, scripting, etc)
* Kotlin version upgrade to 1.3.70
* WaveBeans API is no longer exposing Kotlin Experimental API.

Version 0.0.2 on 2020-03-10
------

* CLI: Using kotlinc to compile script.
* Documentation: restructured to publish on [wavebeans.io](https://wavebeans.io)
* HTTP API: introduced [HTTP interface](/docs/user/http/readme.md) for accessing internal resources
* Table: added [querying](/docs/user/api/outputs/table-output.md#querying) over HTTP API
* Inputs: using [List as input](/docs/user/api/inputs/list-as-input.md)
* Operations: merge operation can [merge streams of different types](/docs/user/api/operations/merge-operation.md#using-with-two-different-input-types)
* Output: writing samples to in-memory [table](/docs/user/api/outputs/table-output.md) for later [querying](/docs/user/api/outputs/table-output.md#querying)

Version 0.0.1 on 01/31/2020
------

This is the very first release of WaveBeans. It is considered to be Alpha version -- free to use but without any guarantees regarding quality and if API remains the same.

What's being released:

* Inputs: 
    * [mono wav-files](/docs/user/api/inputs/wav-file.md)
    * [sine and sweep sines](/docs/user/api/inputs/sines.md)
    * [custom functions](/docs/user/api/inputs/function-as-input.md)
* Operations:
    * [basic arithmetic operations on streams](/docs/user/api/operations/arithmetic-operations.md)
    * [change amplitude of Sample stream](/docs/user/api/operations/change-amplitude-operation.md)
    * [FFT analysis](/docs/user/api/operations/fft-operation.md)
    * [Transformations with map function](/docs/user/api/operations/map-operation.md)
    * [Mixing stream using merge function](/docs/user/api/operations/merge-operation.md)
    * [Projections on streams](/docs/user/api/operations/projection-operation.md)
    * [Trimming infinite streams](/docs/user/api/operations/trim-operation.md)
    * [Windowing over the stream](/docs/user/api/operations/window-operation.md)
* Outputs
    * [mono wav-file](/docs/user/api/outputs/wav-output.md)
    * [CSV](/docs/user/api/outputs/csv-outputs.md)
    * [/dev/null](/docs/user/api/outputs/dev-null-output.md)