package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.api.ActorsApi
import com.NOSQL.NOSQL.model.generated.Actor
import com.NOSQL.NOSQL.model.generated.ActorCountResponse
import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorCreateResponse
import com.NOSQL.NOSQL.model.generated.ActorMediaType
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.MediaUploadResponse
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.service.ActorService
import com.NOSQL.NOSQL.service.MediaService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class ActorController(
    private val actorService: ActorService,
    private val mediaService: MediaService,
    @param:Value("\${app.http.cache-control.media-max-age-seconds:300}")
    private val mediaCacheMaxAgeSeconds: Long
) : ActorsApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun v1ActorCreatePost(actorCreate: ActorCreate): ResponseEntity<ActorCreateResponse> {
        log.info("POST /v1/actors {} {}", actorCreate.firstName, actorCreate.lastName)
        val response = actorService.create(actorCreate)
        return ResponseEntity.status(201).body(response)
    }

    override fun v1ActorByIdGet(id: String): ResponseEntity<Actor> {
        log.info("GET /v1/actors/{}", id)
        val actor = actorService.getById(id)
        return ResponseEntity.ok(actor)
    }

    override fun v1ActorByIdDelete(id: String): ResponseEntity<Unit> {
        log.info("DELETE /v1/actors/{}", id)
        actorService.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    override fun v1ActorsGet(
        gender: Gender?,
        ageFrom: Int?,
        ageTo: Int?,
        weightMin: Int?,
        weightMax: Int?,
        heightMin: Int?,
        heightMax: Int?,
        activityYearFrom: Int?,
        activityYearTo: Int?,
        universityId: String?,
        theatre: String?,
        title: Title?,
        hairColor: String?,
        eyeColor: String?,
        genres: List<String>?,
        name: String?,
        limit: Int,
        offset: Int
    ): ResponseEntity<List<Actor>> {
        log.info("GET /v1/actors gender={} limit={} offset={} theatre={} universityId={} name={}", gender, limit, offset, theatre, universityId, name)
        val list = actorService.findAll(
            gender = gender,
            ageFrom = ageFrom,
            ageTo = ageTo,
            weightMin = weightMin,
            weightMax = weightMax,
            heightMin = heightMin,
            heightMax = heightMax,
            activityYearFrom = activityYearFrom,
            activityYearTo = activityYearTo,
            universityId = universityId,
            theatre = theatre,
            title = title,
            hairColor = hairColor,
            eyeColor = eyeColor,
            genres = genres,
            name = name,
            limit = limit,
            offset = offset
        )
        return ResponseEntity.ok(list)
    }

    override fun v1ActorsCountGet(): ResponseEntity<ActorCountResponse> {
        log.info("GET /v1/actors/count")
        return ResponseEntity.ok(ActorCountResponse(total = actorService.countTotal()))
    }

    override fun v1ActorMediaUploadPost(
        id: String,
        file: Resource,
        type: ActorMediaType,
        caption: String?
    ): ResponseEntity<MediaUploadResponse> {
        log.info("POST /v1/actors/{}/media type={}", id, type)
        val filename = file.getFilename() ?: "file"
        val response = mediaService.upload(
            actorId = id,
            inputStream = file.inputStream,
            filename = filename,
            contentType = null,
            type = type,
            caption = caption
        )
        val status = when {
            response.status == MediaUploadResponse.Status.ok -> 201
            response.errorCode == "ACTOR_NOT_FOUND" -> 404
            else -> 400
        }
        return ResponseEntity.status(status).body(response)
    }

    override fun v1MediaByIdGet(actorId: String, mediaId: String): ResponseEntity<Resource> {
        log.info("GET /v1/actors/{}/media/{}", actorId, mediaId)
        val resource = mediaService.getResource(actorId, mediaId)
        return if (mediaCacheMaxAgeSeconds > 0) {
            ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(mediaCacheMaxAgeSeconds, TimeUnit.SECONDS))
                .body(resource)
        } else {
            ResponseEntity.ok(resource)
        }
    }
}
