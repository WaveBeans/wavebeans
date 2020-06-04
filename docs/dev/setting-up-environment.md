Settting up environment
=======

The project is developed using Intelliji Idea. Community Edition should work fine. All further steps assume you have it already installed.

Preliminary steps
----

* Make sure you have Java 8 installed (the JDK version is as an example, Oracle JDK will do either):

```bash
> java -version
openjdk version "1.8.0_252"
OpenJDK Runtime Environment (AdoptOpenJDK)(build 1.8.0_252-b09)
OpenJDK 64-Bit Server VM (AdoptOpenJDK)(build 25.252-b09, mixed mode)
```

* Clone repository

```bash
git clone git@github.com:WaveBeans/wavebeans.git
```

* Make your first build and wait test to pass. No special requirements here.

```bash
./gradlew build
```

Install plugins
----

A few extra plugins you need to make sure you have installed.

* To run tests using IDE: [Spek Framework plugin](https://plugins.jetbrains.com/plugin/10915-spek-framework).
* For protobuf support: [Protobuf support plugin](https://plugins.jetbrains.com/plugin/8277-protobuf-support)

Import project
----

To import the project just open via `File > Open` in IDE the `build.gradle.kts` file in the root directory of the project, select `Open as project`. Select to use gradle wrapper.

Setting up build configurations
----

A few basic configurations are handy to create. Especially for running tests. As by the time of writing this there was no way found to run all tests at once, as well as using Spek plugin. So you need to cre JUnit runner and repeat steps for all projects (`lib`, `exe`, `cli`, `http`).

* Create configuration, then select `JUnit`.
* Name configuration, for example `LIB tests`.
* Select `Test kind: All in package`.
* Select `Search for tests: In single module`.
* Select `Use classpath or module: io.wavebeans.lib.test` (or corresponding to the project).
* Add additional VM option `-DSPEK_TIMEOUT=0` as some tests are taking more than default 10 seconds timeout and failing weirdly.

While working on Protobuf stuff
----

IDE doesn't currently build automatically proto-files, but all source folders are configured correctly. Every time you change them (or cleaned the project) regenerate them by running gradle command:

```bash
./gradlew clean generateProto
```