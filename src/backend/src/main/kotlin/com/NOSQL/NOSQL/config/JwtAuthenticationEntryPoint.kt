package com.NOSQL.NOSQL.config

import com.NOSQL.NOSQL.model.generated.UnauthorizedError
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {
    private val objectMapper = ObjectMapper()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val message =
            request.getAttribute(ATTR_ERROR_MESSAGE) as? String
                ?: "Authorization header required: Bearer <token>"
        val body = UnauthorizedError(error = "Unauthorized", message = message)
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(body))
    }

    companion object {
        const val ATTR_ERROR_MESSAGE = "auth.errorMessage"
    }
}
