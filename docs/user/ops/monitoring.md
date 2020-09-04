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
samplesProcessedOnOutputMetric.collector(facilitatorsLocations, refreshIntervalMs = 5000, granularValueInMs = 60000)
```

That will automatically start the collection of the metric from downstream facilitators in the separate thread.