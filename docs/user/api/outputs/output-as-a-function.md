# Output as a function

Sometimes you may want to process the output in a way which is not provided by the framework, e.g. custom file format, or remote network endpoint, or just store it in memory. For that purpose you may implement the output as a [function](../functions.md). It is as easy as call `.out()` on the stream of any type:

```kotlin
stream.out { /* handle the samples your own way */ }
```

The argument of the function is of type `io.wavebeans.lib.io.WriteFunctionArgument`, which has the following fields:
* `sampleClazz` (`KClass<T>`) -- the class of the sample for convenience.
* `sampleIndex` (`Long`) -- the 0-based global index of the sample.
* `sampleRate` (`Float`) -- the sample rate the output is being evaluated with.
* `sample` (`T?`) -- the nullable depending on the phase `phase` sample value.
* `phase` (`WriteFunctionPhase`) -- the phase of the writing routine: `WRITE`, `END`, `CLOSE`. Phase describes where is the writer currently is: 
    * `WRITE` tells that the writer is currently getting the input signal, and expect it to process. The `sample` field is never `null` in this case.
    * `END` tells that the writer has reached the end of the input stream, but the writer has been called. May not be called in some cases (i.e. the writer's write function is stopped calling before the writer hit on the end of the stream, or the stream is endless), or be called more than once (in case that the writer's write function is called after the previous call returned `false`), but during regular execution is being called only once. The `sample` field is `null` in this case.
    * `CLOSE` tells that the writer is being closed. The `sample` field is `null` in this case.

The function expects to return the value of `Boolean` type, that controls the output writer behavior: 
* In the `WRITE` phase if the function returns `true` the writer will continue processing the input, if it returns `false` the writer will stop processing, but anyway `CLOSE` phase will be initiated.
* It doesn't affect anything in other phases.

Here is some example writing into a shared memory storage, it writes the 1 second of 440Hz sine:

```kotlin
/** 
* It's not a proper storage, just to provide an idea. 
* It is an object to be able to use function as lambda. 
*/
object Storage {
    private val list = ArrayList<Sample>()

    fun add(sample: Sample) { list += sample }

    fun list(): List<Sample> = list
}

440.sine() // the stream is infinite, but we'll limit it in the output function
        .out {
            // write only samples within WRITE phase
            if (it.phase == WRITE) Storage.add(it.sample!!)
            // limit with one second of data
            it.sampleIndex / it.sampleRate < 1.0f
        }
```

Running in multi-threaded or distributed mode: by default outputs are evaluated as a single bean and are not parallelized, the function as an output is not exception. That means it is safe to say the output function may have some state in it, though it is not guaranteed that it will be launched in the very same thread every time. One more thing, if the stream is evaluated sequentially a few times in a row within the same process routine, the function is created only once, so the state should take this into account.