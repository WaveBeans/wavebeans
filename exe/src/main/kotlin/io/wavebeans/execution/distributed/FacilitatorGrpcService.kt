package io.wavebeans.execution.distributed

import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import io.wavebeans.communicator.*
import io.wavebeans.execution.*
import io.wavebeans.execution.pod.PodKey

class FacilitatorGrpcService(
        val facilitator: Facilitator
) : FacilitatorApiGrpc.FacilitatorApiImplBase() {

    companion object {
        fun instance(facilitator: Facilitator): FacilitatorGrpcService =
                FacilitatorGrpcService(facilitator)
    }

    override fun call(request: CallRequest, responseObserver: StreamObserver<CallPartialResponse>) {
        responseObserver.handle("$this.call", request) {
            val bushKey = request.bushKey.toBushKey()
            val podKey = PodKey(request.podId, request.podPartition)
            val req = request.request
            facilitator.call(bushKey, podKey, req).use {
                val buffer = ByteArray(8192)
                do {
                    val read = it.read(buffer)
                    if (read > 0) {
                        onNext(
                                CallPartialResponse.newBuilder()
                                        .setBuffer(ByteString.copyFrom(buffer, 0, read))
                                        .build()
                        )
                    }
                } while (read > 0)
            }
        }
    }

    override fun terminate(request: TerminateRequest, responseObserver: StreamObserver<TerminateResponse>) {
        responseObserver.single("$this.terminate", request) {
            facilitator.terminate()
            TerminateResponse.getDefaultInstance()
        }
    }

    override fun startJob(request: StartJobRequest, responseObserver: StreamObserver<StartJobResponse>) {
        responseObserver.single("$this.startJob", request) {
            facilitator.startJob(request.jobKey.toJobKey())
            StartJobResponse.getDefaultInstance()
        }
    }

    override fun stopJob(request: StopJobRequest, responseObserver: StreamObserver<StopJobResponse>) {
        responseObserver.single("$this.stopJob", request) {
            facilitator.stopJob(request.jobKey.toJobKey())
            StopJobResponse.getDefaultInstance()
        }
    }

    override fun jobStatus(request: JobStatusRequest, responseObserver: StreamObserver<JobStatusResponse>) {
        responseObserver.single("$this.jobStatus", request) {
            val jobKey = request.jobKey.toJobKey()
            JobStatusResponse.newBuilder()
                    .addAllStatuses(facilitator.status(jobKey))
                    .build()
        }
    }

    override fun describeJob(request: DescribeJobRequest, responseObserver: StreamObserver<DescribeJobResponse>) {
        responseObserver.single("$this.describeJob", request) {
            val jobKey = request.jobKey.toJobKey()
            DescribeJobResponse.newBuilder()
                    .addAllJobContent(facilitator.describeJob(jobKey))
                    .build()
        }
    }

    override fun listJobs(request: ListJobsRequest, responseObserver: StreamObserver<ListJobsResponse>) {
        responseObserver.single("$this.describeJob", request) {
            ListJobsResponse.newBuilder()
                    .addAllJobKeys(facilitator.jobs().map { it.toString() })
                    .build()
        }
    }

    override fun uploadCode(request: UploadCodeRequest, responseObserver: StreamObserver<UploadCodeResponse>) {
        responseObserver.single("$this.uploadCode", request) {
            facilitator.registerCode(request.jobKey.toJobKey(), request.jarFileContent.newInput())
            UploadCodeResponse.getDefaultInstance()
        }
    }

    override fun codeClasses(request: CodeClassesRequest, responseObserver: StreamObserver<CodeClassesPartialResponse>) {
        responseObserver.sequence("$this.codeClasses", request) {
            // the list of classes is quite long, it hits on gRPC packet limitation
            val amountOfClassesToSendAtOnce = 1000
            facilitator.startupClasses().asSequence().windowed(amountOfClassesToSendAtOnce, amountOfClassesToSendAtOnce, true)
                    .map {
                        CodeClassesPartialResponse.newBuilder()
                                .addAllClasses(it.map { descriptor ->
                                    CodeClassesPartialResponse.ClassDesc.newBuilder()
                                            .setLocation(descriptor.location)
                                            .setClassPath(descriptor.classPath)
                                            .setCrc32(descriptor.crc32)
                                            .setSize(descriptor.size)
                                            .build()

                                })
                                .build()
                    }
        }
    }

    override fun plantBush(request: io.wavebeans.communicator.PlantBushRequest, responseObserver: StreamObserver<PlantBushResponse>) {
        responseObserver.single("$this.plantBush", request) {
            facilitator.plantBush(request)
            PlantBushResponse.getDefaultInstance()
        }
    }

    override fun registerBushEndpoints(request: io.wavebeans.communicator.RegisterBushEndpointsRequest, responseObserver: StreamObserver<RegisterBushEndpointsResponse>) {
        responseObserver.single("$this.registerBushEndpoints", request) {
            facilitator.registerBushEndpoints(request)
            RegisterBushEndpointsResponse.getDefaultInstance()
        }
    }

    override fun status(request: StatusRequest, responseObserver: StreamObserver<StatusResponse>) {
        responseObserver.single("$this.status", request) {
            StatusResponse.getDefaultInstance()
        }
    }
}