# Monitoring

WaveBeans uses metric approach to be monitored, and can be connected to a variety of data sources, i.e. Prometheus, as well as provides the same metrics for application.

## Metrics

## Prometheus

## Usage within application

WaveBeans provides the collector abstraction that helps collect metrics either during Local or Distributed execution. Collectors are created on per metric basis.

### Distributed mode

To create a collector for a metric in distributed mode:

```kotlin
val facilitatorsLocations = listOf("10.0.0.1:40000", "10.0.0.2:40000")
val collector = samplesProcessedOnOutputMetric.collector(
    downstreamCollectors = facilitatorsLocations, 
    refreshIntervalMs = 5000, 
    granularValueInMs = 60000
)
```

That will automatically start the collection of the metric from downstream facilitators in the separate thread, downstream collectors are created and registered automatically for the specified metric.

Parameters are:
* `downstreamCollectors` - the list of the servers to collect from in format `<host-or-ip-address>:<port>`, in most cases that would be facilitator locations.
* `refreshIntervalMs` - the interval in milliseconds the collector will perform the job with, first iterations are spent to connect to downstream collectors unless all of them are connected, to disable automatic fetching in separate thread specify 0. By default, 5000ms is set.
* `granularValueInMs` - the amount of time the metric will be accumulated for, that means for that period of time singular events won't be distinguishable. By default, is set to 1 min (60000ms), if you set it to 0, it won't do any roll up, though it may affect the memory consumption and overall performance.

To collect all values from the collector, you may call `collectValues()`, which return the list of `TimedValue<T>`, where `T` is the type return by your metric. Once the method is called, the internal state of the collector is cleaned up up to the time point you've specified , so make sure you've stored the values, you won't get them once again.

Once you've finished working with the collector you need to close it to free up all resources:

```kotlin
collector.close()
``` 