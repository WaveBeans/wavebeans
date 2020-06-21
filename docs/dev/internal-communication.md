Internal Communication
=========

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- END doctoc generated TOC please keep comment here to allow auto update -->


All internal calls are performed via gRPC. The `:proto` project encapsulates the protocol protobuf `*.proto` files as well as Client implementations (which are wrappers around cumbersome prtobuf builders mainly) and helper methods for service implementations.

The proto-generated files are not committed to repository, but added into a build classpath whenever they are presented. To generate those files invoke `generateProto` gradle task, also you would need to run it every time you change proto-files. 