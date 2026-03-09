package com.NOSQL.NOSQL.config

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(Throwable::class)
    fun handleAny(e: Throwable): ResponseEntity<Map<String, Any>> {
        if (e is org.springframework.web.server.ResponseStatusException) {
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
