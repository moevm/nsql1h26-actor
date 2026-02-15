package com.NOSQL.NOSQL.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "actors")
data class Actor(
    @Id
    val id: String? = null,
    val name: String,
    val email: String,
    val role: String = "USER",
    val createdAt: LocalDateTime? = LocalDateTime.now()
)
