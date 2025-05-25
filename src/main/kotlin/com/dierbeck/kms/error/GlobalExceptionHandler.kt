package com.dierbeck.kms.error

import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Produces
@Singleton
@Requires(classes = [Throwable::class])
class GlobalExceptionHandler : ExceptionHandler<Throwable, HttpResponse<ErrorResponse>> {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    override fun handle(request: HttpRequest<*>, exception: Throwable): HttpResponse<ErrorResponse> {
        logger.error("Unhandled exception", exception)
        
        return when (exception) {
            is CustomerNotFoundException -> HttpResponse.notFound(
                ErrorResponse(
                    error = exception.message ?: "Customer not found",
                    path = request.path,
                    status = HttpStatus.NOT_FOUND.code
                )
            )
            
            is CustomerServiceException -> HttpResponse.serverError(
                ErrorResponse(
                    error = exception.message ?: "Service error",
                    path = request.path
                )
            )
            
            else -> HttpResponse.serverError(
                ErrorResponse(
                    error = "Something went wrong!",
                    path = request.path
                )
            )
        }
    }
}