package io.wavebeans.http

import io.javalin.Javalin
import io.javalin.http.Context
import mu.KotlinLogging

interface JavalinApp {
    fun setUp(javalin: Javalin)
}

class DefaultJavalinApp(
    private val applications: List<(Javalin) -> Unit>
) : JavalinApp {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun setUp(javalin: Javalin) {
        applications.forEach { it(javalin) }

        javalin.exception(Exception::class.java) { e, ctx ->
            log.error(e) { "Exception raised: $ctx" }
        }

        javalin.exception(BadRequestException::class.java) { e, ctx ->
            log.debug(e) { "BadRequestException: $ctx" }
            ctx.result(e.message ?: "Bad Request")
            ctx.status(400)
        }
        javalin.exception(NotFoundException::class.java) { e, ctx ->
            log.debug(e) { "NotFoundException: $ctx" }
            ctx.result(e.message ?: "Not Found")
            ctx.status(404)
        }
    }

}

class BadRequestException(message: String) : Exception(message)

class NotFoundException(message: String) : Exception(message)

fun <T : Any> Context.requiredPath(name: String, converter: (String) -> T?): T =
    converter(this.pathParam(name))
        ?: throw BadRequestException("Path parameter $name `${this.pathParam(name)}` is not recognized")

fun <T : Any> Context.requiredQuery(name: String, converter: (String) -> T?): T {
    return optionalQuery(name, converter)
        ?: throw BadRequestException("Query parameter $name `${this.queryParam(name)}` is not recognized")
}

fun <T : Any> Context.optionalQuery(name: String, converter: (String) -> T?): T? =
    this.queryParam(name)?.let {
        converter(it) ?: throw BadRequestException("Query parameter $name `${this.queryParam(name)}` is not recognized")
    }