package io.wavebeans.communicator

import io.grpc.stub.StreamObserver
import mu.KotlinLogging

/**
 * Handles response as a single element and send each it via calling [StreamObserver.onNext] and
 * finishes request correctly either in case of success or error.
 *
 * It runs the block and then call [StreamObserver.onCompleted] if it is succeeded,
 * or [StreamObserver.onError] in case of the error.
 *
 * @param methodName the method name, i.e `MyGrpcService.doSomething` to refer in logging.
 * @param request the initial request to use in logging.
 * @param block the function to run replacing `this` with [StreamObserver]. Should return single element to send all at once.
 * @param REQUEST the type of the gRPC request being handled
 * @param RESPONSE the type of the gRPC response to be returned
 */
fun <REQUEST, RESPONSE> StreamObserver<RESPONSE>.single(
        methodName: String,
        request: REQUEST,
        block: StreamObserver<RESPONSE>.() -> RESPONSE
) = this.handle(methodName, request) { onNext(block()) }

/**
 * Handles response as a sequence and send each elements via calling [StreamObserver.onNext] and
 * finishes request correctly either in case of success or error. Worth to mention, [StreamObserver.onNext]
 * is called for every element, so if the error is happenned in the middle some of the elements
 * may have been sent depedning on underlying gRPC implementation. The gRPC function response should be defined as
 * `stream` to support that
 *
 * It runs the block and then call [StreamObserver.onCompleted] if it is succeeded,
 * or [StreamObserver.onError] in case of the error.
 *
 * @param methodName the method name, i.e `MyGrpcService.doSomething` to refer in logging.
 * @param request the initial request to use in logging.
 * @param block the function to run replacing `this` with [StreamObserver]. Should return the sequence of the elements.
 * @param REQUEST the type of the gRPC request being handled
 * @param RESPONSE the type of the gRPC response to be returned
 */
fun <REQUEST, RESPONSE> StreamObserver<RESPONSE>.sequence(
        methodName: String,
        request: REQUEST,
        block: StreamObserver<RESPONSE>.() -> Sequence<RESPONSE>
) = this.handle(methodName, request) { block().forEach { this.onNext(it) } }

/**
 * Handles response of any form finishing up the request correctly in case of either success or error.
 * It runs the block and then call [StreamObserver.onCompleted] if it is succeeded,
 * or [StreamObserver.onError] in case of the error.
 *
 * Important to note, you need to call [StreamObserver.onNext] on your own. Overall consider using higher
 * level functions to handle requests: [single] and [sequence].
 *
 * @param methodName the method name, i.e `MyGrpcService.doSomething` to refer in logging.
 * @param request the initial request to use in logging.
 * @param block the function to run replacing `this` with [StreamObserver].
 * @param REQUEST the type of the gRPC request being handled
 * @param RESPONSE the type of the gRPC response to be returned
 */
fun <REQUEST, RESPONSE> StreamObserver<RESPONSE>.handle(
        methodName: String,
        request: REQUEST,
        block: StreamObserver<RESPONSE>.() -> Unit
) = try {
    block()
    this.onCompleted()
} catch (e: Throwable) {
    KotlinLogging.logger("io.wavebeans.communicator.GrpcHandler").error(e) { "Execution failed: $methodName($request)" }
    this.onError(e)
}
