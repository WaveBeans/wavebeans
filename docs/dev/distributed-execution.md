# Distributed execution

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [What this document is about](#what-this-document-is-about)
- [Overview](#overview)
- [Lifecycle](#lifecycle)
- [Starting Job process detailed](#starting-job-process-detailed)
  - [Prepare for planting](#prepare-for-planting)
  - [Planting under the microscope](#planting-under-the-microscope)
  - [Registering Bush Endpoints](#registering-bush-endpoints)
  - [Starting job and tracking its progress](#starting-job-and-tracking-its-progress)
- [Pods distribution](#pods-distribution)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## What this document is about

The document describes how the distributed excution is being launched and managed, known caveats and development directions. It implies some architecure diagrams and referencing the code. That document is mainly for developers and for those who wants to understand the system better.

## Overview

That section covers definitions specific to distributed execution on top of [system-wide definitions](definitions.md) as well as their role in the execution.

On the figure below shown how different actors relate to each other during that process, and what resources do they manage. Let's go from the top to the bottom.

![Actors Hierarchy][actors-hierarchy]

The top actor in the hierarchy is the **[Overseer](definitions.md#overseer)**. As by its definition it controls the execution of the beans and returns the control to the [Application](definitions.md#application) when the execution is done. As an input it gets the the list of locations of [Facilitators](definitions.md#facilitator) and the list of [Stream](definitions.md#stream) Outputs, as well as bunch of other parameters not valuable at the moment. The first thing, the outputs are used to track down all beans to build a [Topology](definitions.md#topology) and then [Pods](definitions.md#pod). Once [pods are built and ready](building-pods.md), they are [distributed](#how-distribution-happenes) across all facilitators. The overseer is the only place the whole **Distribution plan** is available, so basically it says everybody where to find everything. It is implemented as class `io.wavebeans.execution.distributed.DistributedOverseer`.

**Facilitator** helps Overseer and [Gardener](definitions.md#gardener) with communication over the network as well as allows cross pods communication. It tracks all [Jobs](definitions.md#job) started by any overseer, uploads and registers code to make it available by the gardener during execution. The main class for the facilitator is `io.wavebeans.execution.distributed.Facilitator`. Facilitator creates a **PodDiscovery** resource that is shared across all subordinates. Currently it is created as one for the whole JVM, so if several Facilitators are started they'll share it. **ClassLoader** and **Worker Pool** are also created for the whole JVM instance, and currently have no way to be created otherwise. 

**Gardener** is the one that manages [Bushes](definitions.md#bush). It can have multiple bushes at a time. It keeps tracking which jobs are active and what pods corresponds to it. This is the actor that performs actual planting of the bushes. It provides insignht into Bush status, if it is completed or still working. Gardener instantiates the Pod based on their reference during start up, it uses **ClassLoader** to find specific classes.The class is `io.wavebeans.execution.Gardener`.

The **Bush**. As was mentioned a few times already, the bush covers working set of pods, it groups them together and actually executes Pods. The bush may have one or more pods, some of the pods are active (TickPods), others are passive hence perform computations only when someone called them. Bush encapsualtes the logic of calling one of the overseen pods and providing the result back. As for the overseen pods, it manages their lifecycle, registering it in **PodDiscovery** and unregistering when the bush is being closed. Bush uses **Worker Pool** to execute all tasks.

The executable unit in the distribution (and mutli-threaded) is **Pod**. Pod encasulates a few [Beans](definitions.md#bean) with logic and connect them altogether. Most of the beans have inputs and outputs. So if some of the beans have input that is to the bean in another pod, it is connected seemlessly via [PodProxy](definitions.md#pod-proxy). On the other hand, if the bean doesn't allow to connect any other bean (i.e. terminal beans like Stream Outputs), it is created as a pod with specific type `TickPod`, and this is the type that is "artificially" called by the bush in order to perform some calculations. Once the pod is called it calls other pod all way down to beans without inputs (i.e. Stream Input). 

Pods can be **Remote** or **Local**. Local Pods perform the calculation on **Beans**, while Remote Pods encapsulate calling of Pods located on a different Bush. The call is perfomed via according Facilitator, the information is found usingg **Pod Discovery**. More details on logic of [pods communication](pods-communication.md).

**ClassLoader** resource is owned by **Facilitator**, but is created globally on JVM. It keeps all code loaded for jobs, all classes can be found there. The classes are made inaccessible when job is finished and stopped from the **Overseer**. *Known caveat: the job classes currently are not isolated and easily may interfere with each other, which is bad either from execution perspective (wrong class with the same name picked up) or security (from overseer injected malicious code).* The implementation is not actually a java Class Loader, it can be found in class `io.wavebeans.lib.WaveBeansClassLoader`.

**Worker Pool** is created when **Facilitator** start and torn down when it is closed. It is the shared pool for all tasks performed by **Bushes**, even 1 thread is enough, as execution is perfomed in non-blocking asynchronous mode. *Known caveat: If another faciliator is created within the same JVM it'll rewrite the global setting.* For implementation look for `io.wavebeans.execution.ExecutionThreadPool` interface implementations.
 
**Pod Discovery** is the object that knows where **Pods** can be located -- which Bush and Facilitator. *Known caveat: By default, is created once for JVM and can't be used otherwise as is not propagated to the places which run the discovery.* The immplementation is open class `io.wavebeans.execution.PodDiscovery`.

## Lifecycle

One of most important things to understand: relationship between actors and their lifetime. The figure below represents the lifecycle of the actors during Distributed execution. It looks quite complicated, so explanation right after.

![Lifecycle][lifecycle]

On high level the lifecycle consists of 7 steps:

1. Initialize the environment.
2. Start the [application](definitions.md#application) and plan the execution.
3. Actually distribute the work among machines.
4. Start the execution. Formallity but begin the process.
5. Do the execution and wait for result.
6. Commit the results and finish the application routine (if needed).
7. Tear down the environment unless you won't do something else.

**Initialize enviornment** stands for a standalone action that happens outside of the execution routine. That step assumes you'll start the [Facilitator](definitions.md#facilitator) processes on your own, get the port they've started on, and passed it as a configuration to the application. To create an instance of Distributed Overseer in your application you would require a list of Facilitator locations. So on that step as much Facilitators as you need are started up. 

As shown on the figure Facilitator process also creates and starts the Gardener which later will take care of all bushes with pods on them.

All resources are also initialized: 

* **ClassLoader** is created based on current process class loader and has access to all classes in current runtime. Also at that time the list of loaded classes is fetched so later Overseer may not upload them onto the Facilitator. That functionality is provided via `io.wavebeans.execution.distributed.Facilitator.startupClasses` method.
* **PodDiscovery** is also created but at the moment nothing else is done. Later on it'll be heavily used by Pods and Bushes.
* **Worker pool** is also initialized and started with the number of threads specified via configuration. 

When the environment has started up and ready, we can **start the application**. Application may do a lot of things unrelated to the audio processing, though at some point it'll decide to start it up. At that moment, application already defined the Streams and Facilitator locations are bypassed through. With all that information it creates instance of [Overseer](definitions.md#overseer). At the same time Streams are transformed into pods creating a [Topology](definitions.md#topology) and then it is **[being distributed](#pods-distribution)**. The actual distribution happens a little later when it first gets claimed by next steps.

When the application is ready to proceed further it asks Overseer for **evaluation** and this is when all compication comes into a place. Here we'll cover high level overview of what's happening during start, please follow [appropriate section for deeper dive](#starting-job-process-detailed).

The first thing, the Overseer calls all Facilitators according to the distribution plan asking to **plant the bush** providing pods the bush should handle. Facilitator creates a **Job State** which then will be used to track down the progress and resources, and asks Gardener to take care of the bush. Gardener instantiates all Pods according to provided references, plants the Bush and assignes Pods to it, that registers Pods within **PodDiscovery**. The last thing, Gardener creates a record of the upcoming job and states that he's ready to proceed further. No any processing yet.

Once Overseer has spread all pieces of work amoung Facilitator and Gardeners are ready to work, it gives command to **start** processing, Facilitator bypasses it to Gardener and then it has reached Bushes and Pods. During the start Bushes start asynchonous task to work with TickPods, the **TickPod Futures** are used to track the progress, and they are marked complete when the TickPod signals about it.

The **do execution** step is about actually performing the work. The bush are ticking the TickPods using **Worker Pool**, every tick the Pod performs one iteration of processing involving all dependent Pods (and Beans). Pods are calling each other in order to retrieve the data and those calls fulfilled using the same **Worker Pool**. Pods are not working by themselves just when they called, TickPods are an exception, but they are mainly terminal ones, they have no one to call them naturally.

The TickPods status are tracked via Futures and the status call to Facilitator are got resolved by checking if the future is completed. The status calls are performed from Overseer any other second, and once the TickPods complete the execution the Overseer complete its future, hence the Application can handle the complete stage. The application may do its work while **waiting for result** and that futures are not completed.

When the application gets notified via futures the job is done, to **commit the results** it commands to close to Overseer. That's the signal for Overseer (and thus Gardner) to stop the job, flush all the buffers and finish with all commitments. So technically, close is propagated to all affected Actors down to Pods. The close may take a while and the Application must wait for Facilitator to acknowledge that close is performed, otherwise some data might be incomplete or corrupted. Once it is done, Pods are being destroyed and unregistered from the **PodDiscovery**, Bushes are destroyed then same **Job** on the Gardener and **JobState* on the Facilitator, classes related are also cleared out by Facilitator. The **Job** is no longer trackable.

For the sake of clarity, **Application** may not wait for Futures to complete, hence all TickPods to finishes their processing (it may have no end even). If **Application** decide so, it may interrupt the execution earlier, and that would mean gracefully stop all jobs with the same result and information loss.

The last step is to **tear down environment** happens unrelatively to application and may happen or may not. But eventually Facilitator process should be ended. The only thing that is worth noting here is that it tries to stop gracefully all jobs if they are still running.

## Starting Job process detailed 

Starting the job on Distributed Overseer consists of a few steps so it's worth to dive deeper into the details. During the start all crucial resources are being initialized and it is essential to do this right to make sure the further process goes smoothly. On the figure below is the overall schema of the process, underneath the explanation.

![Job start process][job-start]

### Prepare for planting

First of all, when Application defined the streams, it requires to create an Overseer, providing a set of parameters. One of these parameters are Stream Outputs which are used to backtrack all beans in all streams. When Beans and their Links are retrieved, that combintaion forms a Topology. Topology basically is a JSON document with explanation what beans, which types, and what parameters they were created with, as well as how this beans connect with each other -- which input connected with what output. That topology is optimized and compiled into a set of pods. More about this process in a separate document on [building pods](building-pods.md). When the pods are compiled the [distribution needs to be planned](#pods-distribution). The distribution plan is stored as a property of Overseer, so it could know exactly where specific pod can be found and where to track the progress. 

The next thing, right after the distribution was planned, Oversee starts to **upload the code** to all Facilitators. To avoid excessive uploading and overloading Facilitators with clasees, Overseer should upload only missing classes. To solve that Overseer asks Facilitator about its startup classes and and compares with its own startup classes to upload only classes that are absent on Facilitator. Mainly that includes only application classes and its dependecies. The start up classes are retrieved via ploughing through the process classpath. Some classes may not exist at the moment of start, but if classes needs to be accessed anyway, you would need to make them accessible as `*.class` files in the file system accessible for Overseer and provide the list as a constructor parameter of `DistributedOverseer`. For example, that approach is adopted by Command Line tool as the code is provided as a text file on start up and is compiled using embeddable Kotlin Script compiler, hence classes accessible only in runtime of current JVM. 

While the code is uploaded it's being **registered within ClassLoader** so any operation can easily access it - whether it is a custom bean type, or a sample type, or anything else. The specific classes are isolated to specific Job as Java ClassLoader instance, though the mechanism currently looks for a class across all classloaders as job key isn't known in every single context. For future, it might be considered as important point of improvement.

Once the code is accessible, it's time to **plant the bushes**. According to distribution plan, Overseer tells each Facilitator what Pods needs to be assigned to which Bush, Gardener performs the actual planting. Each Pod and Bush has globally unique key (random UUID), Pod Keys are determined while building the Topology, the Bush Keys are determined during planting. Once the Failitator gets the Bush description it actually performs planting of so called `LocalBush`es. The Bush registers itself and the assigned Pods in the **PodDiscovery**, so Pods on any other Bush can locate them. Pods are also instantiated at that moment.

### Planting under the microscope

Let's stop for a moment to explain how the Pods are being instantiated. To continue further let's take a look at the Pod descriptor knows an `PodRef`. PodRef has the key, internal beans, internal links and pod proxies, see the figure below.

![PodRef Anatomy][podref-anatomy]

**Internal Beans** are the regular beans that were grouped together into the Pod. They are here as `BeanRef` which describes the class type of the bean and its parameters to recreate it. The classes are loaded with the help of **Class Loader**, so at the moment Gardener has the access to all classes uploaded by Overseer. All beans has two main constructor parameters: the inputs and parameter object. The inputs cound can be from 0 to any other numbe, depedning on the Bean type. I.e. all stream inputs has type `io.wavebeans.lib.SourceBean`, which do not require any bean to be sourced, and merge operation has type `io.wavebeans.lib.MultiBean`, which has multiple inputs. More about different types can be found in [building pods article](building-pods.md).

So most of the beans require to have input, which is another bean, and so on all the way to inputs. That relationships are managed via **Internal Links**, that keep all that information. Each Bean has the key, which is unique only for that topology. The link actually two numbers: from and to. Also here worth to mention, if the bean is partitioned, the link also keeps the partition number as that is crucial. [More about partitioning].

But the pod may not contain the SourceBean, which means it should connect to another bean which is in a different Pod. To address that problem **PodProxy** are here to help. PodProxy is the virtual Bean implementation that encapsulates the process of calling the Bean in a different Pod, it knows the Pod, Pod provides only one Bean as an input. It uses **Pod Discovery** to locate where the Pod is and perform the call. [More about Pods communication](pods-communication.md).

As the result of planting, all Beans are created, Pods grouped beans together as executable unit, and Pods is linked together.

### Registering Bush Endpoints

At this point, all Gardeners (via Faciliators) has the code ready to be executed, but still remain one problem. Some of the Pods that should communicate with each other has no information about it and won't be able to find that information in **Pod Discovery**. That just means the pod should perform the remote call over the network to a different node. Only Facilitators are listening to network, and the call performed with their help. Though still not clear where to find the endpoint to call.

Only Overseer knows all locations of all Pods, as it keeps the Distribution Plan. What is happening, for each Facilitator it sends the location of all Pods of every other Faciliator. Faciliator registers that information in **PodDiscovery**, but instead of creating LocalBush, it creates **RemoteBush**. So when the PodDiscovery is asked for a location of the pod, the RemoteBush is returned back and actual call is perfomed using remote call. The caller doesn't even know if something has changed.

### Starting job and tracking its progress

All Bushes and Pods are created, and can locate each other, but not doing anything yet. The last step is to kick them by calling Faciliator to **start the job**. At the same time Overseer launches the single threaded local job to fetch the status from Facilitator every other second. Also that's the point of future recovery from Facilitator failover, and designing Highly Available Overseer if that's required.

## Pods distribution

![Distribution][distribution]

The provided list of outputs are transformed to a [Topology](definitions.md#topology) which then used to [build pods](building-pods.md). In this section is explained how these pods are being distributed.

Distributed overseer, while created, provided with the list of [Facilitators](definitions.md#facilitator) it required to be distributed over. The actual planning performs the Distribution Planner. You can use built-in or provide your own. The distributed overseer has optional constructor parameter `distributionPlanner`. That parameters allows to specify the implementation of interface `io.wavebeans.execution.distributed.DistributionPlanner`. Currently there is one planner implemented that just distributed all pods evenly `io.wavebeans.execution.distributed.EvenDistributionPlanner`.

The idea behind this planner is to be able to distribute based on Facilitator states, i.e. taking into account current capacity and assignments, as well as Bean aware deployment like, for example, inputs and outputs are better spread across different overseers as they may have high IO, or even requires special types of the nodes. All this things Planner can fetch upon start and make a better judgement what to deploy where. And the overseer will blindly follow the lead.



[actors-hierarchy]: assets/distributed-execution-actors-hierarchy.png "Actors Hierarchy"
[distribution]: assets/distributed-execution-distribution.png "Distribution"
[lifecycle]: assets/distributed-execution-lifecycle.png "Lifecycle"
[job-start]: assets/distributed-execution-job-start.png "Job start process"
[podref-anatomy]: assets/distributed-execution-podref-anatomy.png "PodRef anatomy"