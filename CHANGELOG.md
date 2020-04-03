Version 0.0.3 on 2020-04-03
------

* Execution: **Breaking changes** `Overseer.eval()` now returns different type of result. Follow [docs](/docs/user/exe/readme.md)
* CLI: Using Kotlin Scripting as main engine. No need to install Kotlin SDK anymore
* Support of different [window functions](https://en.wikipedia.org/wiki/Window_function):
  * [Documentation](/docs/user/lib/operations/map-window-function.md)
  * Implementation for [Sample](/docs/user/lib/operations/map-window-function.md#stream-of-sample-type) type:
    * [rectangular](https://en.wikipedia.org/wiki/Window_function#Rectangular_window)
    * [triangular](https://en.wikipedia.org/wiki/Window_function#Triangular_window)
    * [blackman](https://en.wikipedia.org/wiki/Window_function#Blackman_window)
    * [hamming](https://en.wikipedia.org/wiki/Window_function#Hann_and_Hamming_windows)
  * Generic implementation for [any type](/docs/user/lib/operations/map-window-function.md#stream-of-any-type)
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
* Table: added [querying](/docs/user/lib/outputs/table-output.md#querying) over HTTP API
* Inputs: using [List as input](/docs/user/lib/inputs/list-as-input.md)
* Operations: merge operation can [merge streams of different types](/docs/user/lib/operations/merge-operation.md#using-with-two-different-input-types)
* Output: writing samples to in-memory [table](/docs/user/lib/outputs/table-output.md) for later [querying](/docs/user/lib/outputs/table-output.md#querying)

Version 0.0.1 on 01/31/2020
------

This is the very first release of WaveBeans. It is considered to be Alpha version -- free to use but without any guarantees regarding quality and if API remains the same.

What's being released:

* Inputs: 
    * [mono wav-files](/docs/user/lib/inputs/wav-file.md)
    * [sine and sweep sines](/docs/user/lib/inputs/sines.md)
    * [custom functions](/docs/user/lib/inputs/function-as-input.md)
* Operations:
    * [basic arithmetic operations on streams](/docs/user/lib/operations/arithmetic-operations.md)
    * [change amplitude of Sample stream](/docs/user/lib/operations/change-amplitude-operation.md)
    * [FFT analysis](/docs/user/lib/operations/fft-operation.md)
    * [Transformations with map function](/docs/user/lib/operations/map-operation.md)
    * [Mixing stream using merge function](/docs/user/lib/operations/merge-operation.md)
    * [Projections on streams](/docs/user/lib/operations/projection-operation.md)
    * [Trimming infinite streams](/docs/user/lib/operations/trim-operation.md)
    * [Windowing over the stream](/docs/user/lib/operations/window-operation.md)
* Outputs
    * [mono wav-file](/docs/user/lib/outputs/wav-output.md)
    * [CSV](/docs/user/lib/outputs/csv-outputs.md)
    * [/dev/null](/docs/user/lib/outputs/dev-null-output.md)