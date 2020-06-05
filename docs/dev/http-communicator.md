HTTP Communicator
========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->


In distributed mode HTTP Service for certain use cases requires to access data from [Facilitators](definitions.md#facilitator). For that purpose [Overseer](definitions.md#overseer) should be able to communicate with HTTP Service and tell where these resources are located and how to get the access to them.

HTTP Service implements so called HTTP Communicator which is gRPC server. When the overseer performs the distribution it tells the HTTP Communicator by calling `io.wavebeans.http.HttpCommunicatorService.registerTable` where to find the table. The HTTP Service create the instance of remote table driver `io.wavebeans.execution.distributed.RemoteTimeseriesTableDriver` which encapsulates the remote calls to Facilitator and mimics the real table, so that HTTP Service may call it as usual by discovering and invoking.

All of the services are working with their own TableRegistry instances. Look on the picture below to have a better understanding.

![HTTP Communicator][http-communicator]

[http-communicator]: assets/http-communicator.png "HTTP Communicator"
