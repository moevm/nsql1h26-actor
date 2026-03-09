package com.NOSQL.NOSQL.repository

import com.NOSQL.NOSQL.model.AdminDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface AdminRepository : MongoRepository<AdminDocument, String> {
    fun findByEmail(email: String): AdminDocument?
}
