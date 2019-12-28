# WaveBeans

A set of tools to process audio signals using Kotlin/Java/Scala/etc. You can either use it via command line or develop your own program which can be run as a part of you application:

* generate signals and new instruments;
* change existing signals, wave-files or audio streams;
* mix different audio stream together;
* research and explore audio signals;
* make preparation for machine learning algorithms.

Everything using one tool in single- or multi-threaded environment, or even distributing computation across multiple nodes (WIP).

## Getting started 

It's important to understand how you're about to use the WaveBeans, however if you're newbie the easiest way would be to use [command line tool](#wavebeans-cli) which provides comprehensive interface to the functionality and allows start working as soon as possible without deep dive.

### Prerequisites

Overall all you need is to have JRE/JDK 8+ installed and configured properly, so `JAVA_HOME` variable points to correct Java home folder. 

### WaveBeans Cli

Command line interface to work with audio files.

Usage example -- generate 1 second of 440Hz sinusoid and store it as wav-file in current directory:

```bash
export FILE="$(pwd)/sine440.wav" && \
  ${WAVEBEANS_CLI_HOME}/bin/wavebeans --execute \
    "440.sine().trim(1000).toMono16bitWav(\"file://$FILE\").out()"
```

Follow [projects documentation](cli/readme.md) and [usage documentation](docs/user/cli/readme.md). 

### WaveBeans Lib

Kotlin library to work with audio and sound files directly in your projects. Compatible with all other JVM languages -- Java, Scala, etc. Worth to mention, its API is used across other tools.

Follow [project documentation](lib/readme.md) and [usage documentation](docs/user/lib/readme.md)

## Installing

### WaveBeans Cli

In order to start using WaveBeans via command line you just need to download the binaries, unzip it and you're ready to go:

{TODO that is just mock}
```bash
curl -oL {TODO link here}/wavebeans-cli-x.y.z.zip
unzip wavebeans-cli-x.y.z.zip
export WAVEBEANS_CLI_HOME=$(pwd)/wavebeans-cli/
``` 

### WaveBeans Lib

If you want to use WaveBeans in your application just add is as a dependency.

Add maven dependency to your project:

{TODO that is just mock}
```groovy
implementation "io.wavebeans:wavebeans-lib:x.y.z"
```

and start using it:

```kotlin
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import java.io.File
import java.lang.Thread.sleep

val file = File("sine440.wav")

val out = 440.sine()
        .trim(1000)
        .toMono16bitWav("file://${file.absolutePath}")

out.writer(44100.0f).use { writer ->
    while (writer.write()) {
        sleep(0)
    }
}
```

## Building from source

Project uses `Gradle` as a build system. Please make sure you installed JDK 8 and Git before doing that.

Then follow the steps:

1. Clone the repository:
    ```bash
    git clone ....
    ```
2. Build using gradle wrapper:
    ```bash
    cd wavebeans/
    ./gradlew build test
    ```

You can find Cli tool under `cli/build/distributions/`.

## Contribution

Any feedback, bug reports, and pull requests are welcome!

For pull requests, please stick to the following guidelines:

* Add tests for any new features and bug fixes. Ideally, each PR should increase the test coverage.
* Put a reasonable amount of comments into the code.
* Separate unrelated changes into multiple pull requests.

Please note that by contributing any code or documentation to this repository (by raising pull requests, or otherwise) you explicitly agree to the [Contributor License Agreement](CONTRIBUTION.md).

## Questions?

If there are any other questions, concerns or suggestions please feel free to use following communication channels:

* Telegram channel: https://t.me/wavebeans