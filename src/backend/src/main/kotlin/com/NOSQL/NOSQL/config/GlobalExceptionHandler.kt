package com.NOSQL.NOSQL.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(e: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> {
        val msg = e.mostSpecificCause?.message ?: e.message ?: "Invalid request body"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf("error" to "Bad Request", "message" to msg),
        )
    }

    @ExceptionHandler(Throwable::class)
    fun handleAny(e: Throwable): ResponseEntity<Map<String, Any>> {
        if (e is ResponseStatusException) {
            throw e
        }
        log.error("Unhandled exception", e)
        val message = e.message ?: e.javaClass.simpleName
        val cause = e.cause?.message
        return ResponseEntity.status(500).body(
            mapOf(
                "error" to "Internal Server Error",
                "message" to message,
                "cause" to (cause ?: "")
            )
        )
    }
}
