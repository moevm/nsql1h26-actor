package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.api.UniversitiesApi
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.model.generated.UniversityCreateResponse
import com.NOSQL.NOSQL.model.generated.UniversitySearchItem
import com.NOSQL.NOSQL.model.generated.UniversityUpdate
import com.NOSQL.NOSQL.service.UniversityService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UniversityController(
    private val universityService: UniversityService
) : UniversitiesApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun v1UniversitiesSearchGet(q: String, limit: Int): ResponseEntity<List<UniversitySearchItem>> {
        log.info("GET /v1/universities/search q='{}' limit={}", q, limit)
        val list = universityService.search(q, limit)
        return ResponseEntity.ok(list)
    }

    override fun v1UniversityCreatePost(universityCreate: UniversityCreate): ResponseEntity<UniversityCreateResponse> {
        log.info("POST /v1/universities name='{}'", universityCreate.name)
        val response = universityService.create(universityCreate)
        return ResponseEntity.status(201).body(response)
    }

    override fun v1UniversityByIdPatch(id: String, universityUpdate: UniversityUpdate): ResponseEntity<UniversityCreateResponse> {
        log.info("PATCH /v1/universities/{}", id)
        val response = universityService.update(id, universityUpdate)
        return ResponseEntity.ok(response)
    }
}
