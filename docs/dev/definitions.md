# Definitions

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Sample](#sample)
- [Bean](#bean)
- [Stream](#stream)
- [Application](#application)
- [Pod](#pod)
- [Topology](#topology)
- [Pod Proxy](#pod-proxy)
- [Bush](#bush)
- [Job](#job)
- [Gardener](#gardener)
- [Facilitator](#facilitator)
- [Overseer](#overseer)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Sample

In the documentation by saying **Sample** it can mean two things: a audio sample which is a measure of the specific point of audio signal, or more frequently, just as an element to be processed similar to audio sample. WaveBeans is aimed to processed sampled singnals regardless of their nature, so sample word just came out naturally. Anyway it should be clear out of the context, or actually don't matter as either way the meaning are kinda interchangeable.

## Bean

**Bean** is the smallest logical unit in the system which represents one operation -- calculators, remappers, generators and so on. Beans have connections between each other -- **links**. 

Techincally, beans are connected using inputs. The number of inputs of the bean is predefined by the operaition, for example StreamInput has no inputs at all as it generates samples, Merge operation requires two input sources.

Bean outputs are not defined, and appear by design while connecting beans with each other -- if more bean A connects bean B as a input, that metaphorically means bean B has an output of bean A. That number of outputs is not limited, hence bean A can be used as an input for any number of beans.

## Stream

The **Stream** is a collection of beans connected with each other with only one purpose: process the sequence of the samples, alter them and store the result. It is very similar concept to Java Stream or Kotlin Sequence. WaveBeans implies running of stream operations (Beans) on demand, so the terminal operations are essential. 

## Application

**Application** is usually referred as a user developed program that uses WaveBeans API and capabilities. It is obviously outside of the scope of the documentation, though it is important to define the integration points and guidelines.

## Pod

**Pod** is the smallest operational unit in the system. Pod groups a few interconnected beans as one operation that needs to be scheduled. Also it provides the operation approach that work either in [multi-threaded](../user/exe/readme.md#multi-threaded-mode) or [distributed](../user/exe/readme.md#distributed-mode) modes. Pods are not used for [single-threaded exectuion](../user/exe/readme.md#single-threaded-mode) nor evaluation via [writer](../user/exe/readme.md#using-writers) nor [sequence](../user/exe/readme.md#using-sequence). 

Reasonable question: why Bean can't work the same way? Pod have a few optimization that it could apply on top of existing bean execution, but that comes at a cost. And just simple operations are more expensive. It is just not very efficient to use a lot of pods. As a rule of thumb -- less pods the better.

## Topology

**Topology** is a descriptor of all pods, their links and pod internal like beans and internal links. **Topology** is a document. More precisely, JSON document. It is built by back tracking the stream based on their terminal operation (Stream Outputs). Later on it is used for [partitioning](partitioning.md) and [distribution planning](distributed-execution.md#pods-distribution).

## Pod Proxy

While transforming Beans into Pods, Beans should have a way to call properly other Beans they were connect with. That is solved with **PodProxy**. It serves as a virtual Bean and connects Beans of different Pods allowing proper communication. 

For example, as a BeanStream proxy, it implements reading samples as a sequence, it uses internally the PodProxyIterator which calls Bean either locally or remotely depending on their physical location. As well as reads a few buckets at once, stores them as a buffer and streams elements one by one out of this buffer. 

## Bush

**Bush** is the set of Pods as well as some API for manage them conveniently. Bush manages all Pods assigned to it performing call operation or generating tick for so called TickPods.

## Job

**Job** is a task to evaluate the Topology. It is used to track the execution mainly in [distributed](../user/exe/readme.md#distributed-mode) mode as that mode may imply sharing of the same resources among several calculations.

## Gardener

**Gardener** is the abstraction layer that allows to track the Job status and manages several Bushes assigned to that Job. It is used for [multi-threaded](../user/exe/readme.md#multi-threaded-mode) or [distributed](../user/exe/readme.md#distributed-mode) execution.

## Facilitator

**Facilitator** is basically the Process and Network interface to the Gardener. It provides communication layer between Gardeners while being executed in [distributed](../user/exe/readme.md#distributed-mode) mode.

## Overseer

Overseer is the main execution point for the Application. It prepares the launch of the evaluation of the stream, controls further execution and provides status updates. There are 3 main overseers: [single-threaded](../user/exe/readme.md#single-threaded-mode), [multi-threaded](../user/exe/readme.md#multi-threaded-mode) and [distributed](../user/exe/readme.md#distributed-mode). All of them have different capabilities and as a result internal flows. Though the common interface is simple and unified.