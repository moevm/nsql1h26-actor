package com.NOSQL.NOSQL.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "admins")
data class AdminDocument(
    @Id
    val id: String? = null,
    @Indexed(unique = true, name = "admins_email_unique")
    val email: String,
    val passwordHash: String,
    val createdAt: Instant? = null
)
