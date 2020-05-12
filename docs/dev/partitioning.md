Partitioning
=======

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

The topology is being partitioned wherever it is capable of. By default all Beans are partiotionable, some that are not partiotinable implement interface `io.wavebeans.lib.SinglePartitionBean`:

![Partitioning Topology][partitioning-topology]

Splitting stream into partitions, processing them independently, and merging partitioned streams back to one solid stream:

![Partitioning the stream][partitioning-stream]

* [TODO provide text with more explanation]

[partitioning-topology]: assets/partitioning-topology.png "Partitioning Topology"
[partitioning-stream]: assets/partitioning-stream.png "Partitioning the stream"
