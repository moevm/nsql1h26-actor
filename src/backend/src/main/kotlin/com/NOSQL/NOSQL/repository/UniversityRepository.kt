package com.NOSQL.NOSQL.repository

import com.NOSQL.NOSQL.model.UniversityDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UniversityRepository : MongoRepository<UniversityDocument, String>
