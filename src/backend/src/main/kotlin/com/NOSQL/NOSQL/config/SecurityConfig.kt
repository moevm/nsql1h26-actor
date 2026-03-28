package com.NOSQL.NOSQL.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.POST, "/v1/universities").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/v1/universities/*").authenticated()
                    .requestMatchers(HttpMethod.POST, "/v1/actors").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/v1/actors/*").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/v1/actors/*").authenticated()
                    .requestMatchers(HttpMethod.POST, "/v1/actors/*/media").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/v1/actors/*/media/*").authenticated()
                    .anyRequest().permitAll()
            }
        return http.build()
    }
}
