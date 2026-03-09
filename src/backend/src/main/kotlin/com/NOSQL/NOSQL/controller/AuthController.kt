package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.api.AuthApi
import com.NOSQL.NOSQL.model.generated.LoginRequest
import com.NOSQL.NOSQL.model.generated.LoginResponse
import com.NOSQL.NOSQL.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService
) : AuthApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun v1AuthLoginPost(loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        log.info("POST /v1/auth/login email={}", loginRequest.email)
        val response = authService.login(loginRequest)
        return ResponseEntity.ok(response)
    }
}
