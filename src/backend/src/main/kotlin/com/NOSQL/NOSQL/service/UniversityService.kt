package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.UniversityDocument
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.model.generated.UniversityCreateResponse
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UniversityService(
    private val universityRepository: UniversityRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(create: UniversityCreate): UniversityCreateResponse {
        log.info("Creating university: {}", create.name)
        val doc = UniversityDocument(
            name = create.name,
            shortName = create.shortName,
            oldNames = create.oldNames
        )
        val saved = universityRepository.save(doc)
        log.info("University created with id={}", saved.id)
        return UniversityCreateResponse(
            id = saved.id,
            status = UniversityCreateResponse.Status.ok,
            errorCode = null
        )
    }
}
