package com.dierbeck.kms.error

import io.micronaut.http.HttpStatus
import io.micronaut.serde.annotation.Serdeable

// DTO for error responses
@Serdeable
data class ErrorResponse(
    val error: String,
    val path: String? = null,
    val status: Int = HttpStatus.INTERNAL_SERVER_ERROR.code
)