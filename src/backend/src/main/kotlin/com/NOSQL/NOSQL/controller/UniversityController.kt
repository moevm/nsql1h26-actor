package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.api.UniversitiesApi
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.model.generated.UniversityCreateResponse
import com.NOSQL.NOSQL.service.UniversityService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UniversityController(
    private val universityService: UniversityService
) : UniversitiesApi {

    override fun v1UniversityCreatePost(universityCreate: UniversityCreate): ResponseEntity<UniversityCreateResponse> {
        val response = universityService.create(universityCreate)
        return ResponseEntity.status(201).body(response)
    }
}
