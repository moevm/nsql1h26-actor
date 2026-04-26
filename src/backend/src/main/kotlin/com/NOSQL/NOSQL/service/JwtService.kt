package com.NOSQL.NOSQL.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
) {
    private val key by lazy { Keys.hmacShaKeyFor(secret.encodeToByteArray()) }

    fun generateToken(
        subject: String,
        expirationSeconds: Long,
    ): String {
        val exp = Date(System.currentTimeMillis() + expirationSeconds * 1000)
        return Jwts
            .builder()
            .subject(subject)
            .expiration(exp)
            .signWith(key)
            .compact()
    }

    fun parseSubject(token: String): String =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload.subject
}
