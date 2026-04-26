package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.LoginRequest
import com.NOSQL.NOSQL.model.generated.LoginResponse
import com.NOSQL.NOSQL.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    @Value("\${app.jwt.expiration-seconds}") private val expirationSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun login(request: LoginRequest): LoginResponse {
        val admin =
            adminRepository.findByEmail(request.email.trim().lowercase())
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        if (!passwordEncoder.matches(request.password, admin.passwordHash)) {
            log.warn("Failed login for email={}", request.email)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }
        val token = jwtService.generateToken(admin.id!!, expirationSeconds)
        log.info("Login success for email={}", request.email)
        return LoginResponse(token = token, expiresIn = expirationSeconds.toInt())
    }
}
