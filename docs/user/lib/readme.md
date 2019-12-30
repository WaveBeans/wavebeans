WaveBeans API reference
==========

Overview
---------

WaveBeans provides the one atomic entity called a Bean which may perform operations. There are few different types of beans:

1. `SourceBean` -- the bean that has only output, the one you can read from. Such beans are [Inputs](#inputs) for example
2. A `Bean`, which can have one or more input or outputs. This basically are operator that allows you alter the stream and merge different streams together
3. `SinkBean` -- the bean has no outputs, this is the ones that dumps the audio samples onto disk or something like this.

The samples are starting their life in SourceBean then by following a mesh of other Beans which changes them are getting stored or distributed by SinkBean.

WaveBeans uses declarative way to represent the algorithm, so you first define the way the samples are being altered or analyzed, then it's being executed in most efficient way. That means, that effectively SinkBean are pulling out data out of  

Inputs
--------

Inputs allow you to generate some data that the whole other system will then process, alter, slice and dice. Whatever is required. You can choose to read the input from file like WAV file, or you may generate the input based on some mathematical function like sine.

There a few different types of inputs, you may read more in specific:

* [sine](inputs/sines.md)
* [wav-files](inputs/wav-file.md)
* [custom defined function](inputs/function.md)