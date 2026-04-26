package com.NOSQL.NOSQL.config

import com.NOSQL.NOSQL.model.AdminDocument
import com.NOSQL.NOSQL.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@Order(1)
class AdminSeeder(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${app.admin.seed-email:admin@example.com}")
    private lateinit var seedEmail: String

    @Value("\${app.admin.seed-password:admin}")
    private lateinit var seedPassword: String

    override fun run(args: ApplicationArguments) {
        if (adminRepository.count() > 0) return
        val email = seedEmail.trim().lowercase()
        val passwordHash = passwordEncoder.encode(seedPassword)
            ?: throw IllegalStateException("PasswordEncoder.encode returned null")
        adminRepository.save(
            AdminDocument(
                email = email,
                passwordHash = passwordHash,
                createdAt = java.time.Instant.now()
            )
        )
        log.info("Seeded default admin: {}", email)
    }
}
