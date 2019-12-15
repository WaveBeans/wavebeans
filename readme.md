# WaveBeans

A set of project to process audio signals on JVM.

## WaveBeans Lib

Kotlin library to work with sound and sound files directly in your projects. Compatible with all other JVM languages -- Java, Scala, etc.

## WaveBeans CLI

Command line interface to edit sound files.

Usage example (generate 1 second of sinusoid into the file)

```bash
export FILE="$(pwd)/sine440.wav" && \
  ${WAVEBEANS_CLI_HOME}/bin/wavebeans --execute \
    "440.sine().trim(1000).toMono16bitWav(\"file://$FILE\").out()"
```

Follow [projects docs](cli/readme.md) and [usage docs](cli/docs/readme.md).


## WaveBeans Cluster

Running audio processing in distributed mode.