Building pods
=======

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

The [Pods](definitions.md#pod) are build right after the [Topology](definitions.md#topology) was [partitioned](partitioning.md).

![Building Pods from Beans][pod-building]

This is how communication between pods of the same partition happens:

![Streaming Pod communication schema][pod-building-streaming]

This is how communication happens while splitting stream to different partitions between pods:

![Splitting Pod communication schema][pod-building-splitting]

This is how communication happens while merging streams of pods of the different partitions:

![Merging Pod communication schema][pod-building-merging]


* [TODO provide text with explanation]
* [TODO describe bean types , i.e single, source, etc]

[pod-building]: assets/pod-building.png "Building Pods from Beans"
[pod-building-streaming]: assets/pod-building-streaming.png "Streaming Pod communication schema"
[pod-building-splitting]: assets/pod-building-splitting.png "Splitting Pod communication schema"
[pod-building-merging]: assets/pod-building-merging.png "Merging Pod communication schema"
