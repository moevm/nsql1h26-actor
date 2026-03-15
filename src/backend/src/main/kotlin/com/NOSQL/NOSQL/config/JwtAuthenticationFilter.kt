package com.NOSQL.NOSQL.config

import com.NOSQL.NOSQL.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!isProtectedPath(request)) {
            filterChain.doFilter(request, response)
            return
        }
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            request.setAttribute(JwtAuthenticationEntryPoint.ATTR_ERROR_MESSAGE, "Требуется заголовок Authorization: Bearer <token>")
            filterChain.doFilter(request, response)
            return
        }
        val token = authHeader.removePrefix("Bearer ").trim()
        try {
            val subject = jwtService.parseSubject(token)
            val auth = UsernamePasswordAuthenticationToken(subject, null, emptyList<org.springframework.security.core.GrantedAuthority>()).apply {
                details = WebAuthenticationDetailsSource().buildDetails(request)
            }
            SecurityContextHolder.getContext().authentication = auth
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            request.setAttribute(JwtAuthenticationEntryPoint.ATTR_ERROR_MESSAGE, "Невалидный или истёкший токен")
            filterChain.doFilter(request, response)
        }
    }

    private fun isProtectedPath(request: HttpServletRequest): Boolean {
        val path = request.requestURI?.removePrefix(request.contextPath.orEmpty())?.takeUnless { it.isEmpty() } ?: request.requestURI ?: return false
        return when {
            request.method == "POST" && path == "/v1/universities" -> true
            request.method == "POST" && path == "/v1/actors" -> true
            request.method == "POST" && path.matches(Regex("^/v1/actors/[0-9a-fA-F]{24}/media$")) -> true
            request.method == "DELETE" && path.matches(Regex("^/v1/actors/[0-9a-fA-F]{24}$")) -> true
            else -> false
        }
    }
}
