package com.NOSQL.NOSQL.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "universities")
data class UniversityDocument(
    @Id
    val id: String? = null,
    val name: String,
    val shortName: String? = null,
    val oldNames: List<String>? = null,
)
