# Command Line Interface

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Overview](#overview)
- [Requirements](#requirements)
- [Installation instructions](#installation-instructions)
- [Usage](#usage)
  - [Basic example](#basic-example)
  - [Parameters explained](#parameters-explained)
  - [Writing scripts](#writing-scripts)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

WaveBeans CLI is the tool to automate various tasks related to audio signal processing. What it does: it allows you to write the program on Kotlin Script and run it via console in different modes: as local tool, on local distributed environment, or (potentially) on remote cluster. Depending on your task you may benefit or not from running task Locally in Distributed mode, where the execution is launched within defined number of threads, by default, the program is evaluated in a single thread.

## Requirements

The tool requires to have JRE 8+ installed and be configured properly. Please follow installation instructions.

## Installation instructions

* Download the binaries from https://dl.bintray.com/wavebeans/wavebeans/cli/wavebeans.zip

```bash
 curl -LO https://dl.bintray.com/wavebeans/wavebeans/cli/wavebeans.zip
```

* Unpack them

```bash
unzip wavebeans.zip
```

* To avoid providing a full path every time make an alias to WaveBeans CLI

```bash
echo "alias wavebeans=$(pwd)/wavebeans/bin/wavebeans" >> ~/.zshrc
```

* or add it to PATH variable to be located automatically:

```bash
echo "PATH=\$PATH:$(pwd)/wavebeans/bin/" >> ~/.zshrc
```

* Restart the shell and try running the tool, you should see the help output:

```bash
wavebeans
```

## Usage

### Basic example 

Let's try to write a small program and launch it. The program as simple as generate 10 seconds of sinusoid and store it into the `.wav` file.
 
```kotlin
440.sine().trim(1000).toMono16bitWav("file:///path/to/the/file") // define what you want to do 
    .out() // register output stream!
```

To run this program you have two options:

1. Pass it as inline parameter:

```bash
export FILE="$(pwd)/sine440.wav" && wavebeans --execute "440.sine().trim(1000).toMono16bitWav(\"file://$FILE\").out()"
```

2. Or store the script into the file and execute the file

```bash
export FILE="$(pwd)/sine440.wav" && echo "440.sine().trim(1000).toMono16bitWav(\"file://$FILE\").out()" > script.kts 
wavebeans -execute-file script.kts
```

Either way you'll find `sine440.wav`created in the same directory you're in.

### Parameters explained

You can always find the quick help in the tool itself by providing `-h` or `--help` as a parameters, but here are some quick explanation.

**Execute**

As was stated above, you may execute inline script (`-e` or `--execute`) or script from the file (`-f` or `--execute-file`) which basically has no functional difference. The API is fully covered in the LIB. **The most important thing which makes LIB functionality and CLI functionality different is that for the output to take effect you must add `.out()` to register your output, otherwise just nothing gonna happen. You need to call `.out()` for evry single output stream you define.**

**Run mode**

You may choose in what mode to run (`-m` or `--run-mode`). By default you'll run it just in single thread mode (`local`), but you may chooose to run in multi-threaded environment (`local-distributed`).

For `local-distributed` mode you need to pass additional parameters:
* how many partitioned your processing topology will be tried to split up to: `-p` or `--partitions`. You may benefit from using it, but that is not guaranteed. Each partition is processed separately, the stream is broken down by chunks of a few hundreds samples, which are processed in parallel. Not all of the operations support partitioning, so you may end up just using one partition even if you specify more.
* to be able to benefit from parallel processing you need to specify number of threads to use: `-t` or `--threads`. Each thread will be heavily used, so there is no point of specifying more threads than you have cores available. Also, you need to make sure your processing topology can be split up into specified number if threads, otherwise threads gonna be underutilized.

**More information about execution**

[TODO cover `--time` and `-v`]

### Writing scripts

Script is based on the functionality of the lib and everything provided there can be used. So follow LIB documentation to get idea how to solve one or another problem, it's no different that writing your own JVM application.

**Imports**

WaveBeans CLI tool executes the script written on Kotlin language. It has full support not only for library function but any other Kotlin SDK functionality. By default, WaveBeans and [Kotlin standard](https://kotlinlang.org/docs/reference/packages.html#default-imports) packages are imported, but you can import the one you need, just specify them on top of you script as usual:

```kotlin
import java.io.File

val file = File.createTempFile("test", ".csv")
440.sine().trim(Long.MAX_VALUE).toCsv("file://${file.absolutePath}").out()
```

You may specify as many imports as you want, while you're following [imports grammar](https://kotlinlang.org/docs/reference/packages.html#imports)

**Outputs**

WaveBeans processing is declarative. That means you define the processing logic and it is evaluated when terminal action is called. For WaveBeans the terminal action is output. Though it should be connected to the execution environment, in order to be processed.

For the script in order to track the output you should call explicitly `.out()`:

```kotlin

440.sine().trim(1000).toCsv("file:///path/to/file.csv") // specify your handling logic
    .out() // register output to be processed 

```

Otherwise, if you don't call it that lines of code won't be evaluated. Also, if you register output more than once by calling `.out()` a few times, it will be registered as two different output and be evaluated twice.