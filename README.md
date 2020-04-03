# WaveBeans

[ ![Download](https://api.bintray.com/packages/wavebeans/wavebeans/wavebeans/images/download.svg?version=0.0.3) ](https://bintray.com/wavebeans/wavebeans/wavebeans/0.0.3/link)

A set of tools to process audio signals using Kotlin/Java/Scala/etc. You can either use it via command line or develop your own program which can be run as a part of you application:

* generate signals and new instruments;
* change existing signals, wave-files or audio streams;
* mix different audio stream together;
* research and explore audio signals;
* make preparation for machine learning algorithms;
* and many more.

Everything using one tool in single- or multi-threaded environment, or even distributing computation across multiple nodes (WIP).

## Quick links

* [API reference](docs/user/lib/readme.md)
* [Execution reference](docs/user/exe/readme.md)
* [Command line tool reference](docs/user/cli/readme.md)
* [Change log](CHANGELOG.md)

## Getting started 

It's important to understand how you're about to use the WaveBeans, however if you're new to the tool the easiest way would be to use [command line tool](#wavebeans-cli) which provides comprehensive interface to the functionality and allows start working as soon as possible without deep dive.

### Prerequisites

Overall all you need is to have JRE/JDK 8+ installed and configured properly, so `JAVA_HOME` variable points to correct Java home folder. 

### Developing an audio application

WaveBeans is written on Kotlin, but it is compatible with all other JVM languages -- Java, Scala, etc.

If you want to use WaveBeans in your application just add it as a maven dependency. Here is what you would need to add into your `build.gradle` file:

* Register the new maven repository the WaveBeans is hosted in:

```groovy
repositories {
    maven {
        name = "Bintray WaveBeans"
        url = uri("https://dl.bintray.com/wavebeans/wavebeans")
    }
}
```

* Register WaveBeans `exe` and `lib` main libraries, you may not need `exe` if you won't be using execution capabilities:

```groovy
dependencies {
    implementation "io.wavebeans:exe:$wavebeans_version"
    implementation "io.wavebeans:lib:$wavebeans_version"
}
```

* Optionally you may be required to add regular kotlin runtime dependency if you don't have it:
```groovy
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
}
```

And start using it. Just create kotlin-file like this:

```kotlin
import io.wavebeans.execution.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import java.io.File

fun main() {
    // describe what you want compute
    val out = 440.sine()
            .trim(1000)
            .toMono16bitWav("file://" + File("sine440.wav").absoluteFile)

    // this code launches it in single threaded mode,
    // follow execution documentation for details
    LocalOverseer(listOf(out)).use { overseer ->
        if (!overseer.eval(44100.0f).all { it.get() }) {
            println("Execution failed. Check logs")
        }
    }
}
```

For more follow [usage documentation](docs/user/lib/readme.md) and [project documentation](lib/readme.md). 

#### Logging

WaveBeans uses `slf4j` for logging, but it doesn't provide the default logging engine when it is being used inside application. You would need to configure it properly on your own.

## WaveBeans Cli

WaveBeans provides a command line interface to work with audio files. Basically it allows you to run the program written on Kotlin script. Along with it, it provides some smoother API to wrap the code, but that's all the difference comparing to using it inside your application. You can even use Kotlin SDK classes and methods as usual.

For example, let's generate 1 second of 440Hz sinusoid and store it as wav-file in current directory (this is a `bash` script):

```bash
export FILE="$(pwd)/sine440.wav"
./wavebeans --execute "440.sine().trim(1000).toMono16bitWav(\"file://$FILE\").out()"
```

For more information [usage documentation](docs/user/cli/readme.md) and follow [projects documentation](cli/readme.md). 

### Installing Cli

You need to have JRE 8 installed and configured. Also familiarity with Kotlin really helps.

In order to start using WaveBeans via command line you just need to download the binaries, unzip it and you're ready to go:

```bash
curl -LO https://dl.bintray.com/wavebeans/wavebeans/cli/wavebeans.zip
unzip wavebeans.zip
```

Please follow more [precise instructions](/docs/user/cli/readme.md#installation-instructions) 

## Building from source

Project uses [gradle](https://gradle.org/) as a build system. And also, please make sure you installed JDK 8 and Git before doing that. Then follow the steps:

1. Clone the repository:
    
    via `https`
    ```bash
    git clone https://github.com/WaveBeans/wavebeans.git
    ```
    or via `ssh`
    ```bash
    git clone git@github.com:WaveBeans/wavebeans.git
    ```
2. Build and run tests using gradle wrapper:
    ```bash
    cd wavebeans/
    ./gradlew build test
    ```

Among everything else, you can find artifact of Cli tool under `cli/build/distributions/`.

### Using IDE for development

Intellij IDEA is recommended way to develop the framework, however you may find any other IDE like Eclipse with Kotlin plugin work smoothly with no issues.

**Intellij IDEA project set up**

1. Open `build.gradle` as a project inside IDE
2. Choose to use gradle wrapper.
3. Wait for IDE to fetch the project and index everything... and you're pretty much done.

**Running tests in Intellij IDEA**

Project uses [Spek 2](https://www.spekframework.org/) testing framework. You need to install [appropriate Spek plugin](https://plugins.jetbrains.com/plugin/10915-spek-framework/) first. However, at the time of writing you weren't been able to run all tests within one Run configuration, so any module needed to be configured separately via JUnit runner:

* Create JUnit runner, name it, let's say `exe tests`, `lib tests`, `cli tests`
* Test Kind: `All in package`
* Package: `io.wavebeans`
* User classpath of module choose one of: `wavebeans.exe.test`, `wavebeans.lib.test`, or `wavebeans.cli.test`
* Everything else may remain with default values. 

## Contribution

Any feedback, bug reports, and pull requests are welcome!

For pull requests, please stick to the following guidelines:

* Add tests for any new features and bug fixes. Ideally, each PR should increase the test coverage.
* Put a reasonable amount of comments into the code.
* Separate unrelated changes into multiple pull requests.

Please note that by contributing any code or documentation to this repository (by raising pull requests, or otherwise) you explicitly agree to the [Contributor License Agreement](CONTRIBUTION.md).

## Questions?

If there are any other questions, concerns or suggestions please feel free to use following communication channels:

* Open an issue or PR on GitHub.
* Reach through Telegram channel: https://t.me/wavebeans
* Mention on Twitter: [@WaveBeans](https://twitter.com/WaveBeans)