package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.api.ActorsApi
import com.NOSQL.NOSQL.model.generated.Actor
import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorCreateResponse
import com.NOSQL.NOSQL.model.generated.ActorMediaType
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.MediaUploadResponse
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.service.ActorService
import com.NOSQL.NOSQL.service.MediaService
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ActorController(
    private val actorService: ActorService,
    private val mediaService: MediaService
) : ActorsApi {

    companion object{

    }

    override fun v1ActorCreatePost(actorCreate: ActorCreate): ResponseEntity<ActorCreateResponse> {
        val response = actorService.create(actorCreate)
        return ResponseEntity.status(201).body(response)
    }

    override fun v1ActorByIdGet(id: String): ResponseEntity<Actor> {
        val actor = actorService.getById(id)
        return ResponseEntity.ok(actor)
    }

    override fun v1ActorsGet(
        gender: Gender?,
        ageFrom: Int?,
        ageTo: Int?,
        weightMin: Int?,
        weightMax: Int?,
        activityYearFrom: Int?,
        activityYearTo: Int?,
        universityId: String?,
        theatre: String?,
        title: Title?,
        hairColor: String?,
        eyeColor: String?,
        genres: List<String>?,
        limit: Int,
        offset: Int
    ): ResponseEntity<List<Actor>> {
        val list = actorService.findAll(
            gender = gender,
            ageFrom = ageFrom,
            ageTo = ageTo,
            weightMin = weightMin,
            weightMax = weightMax,
            activityYearFrom = activityYearFrom,
            activityYearTo = activityYearTo,
            universityId = universityId,
            theatre = theatre,
            title = title,
            hairColor = hairColor,
            eyeColor = eyeColor,
            genres = genres,
            limit = limit,
            offset = offset
        )
        return ResponseEntity.ok(list)
    }

    override fun v1ActorMediaUploadPost(
        id: String,
        file: Resource,
        type: ActorMediaType,
        caption: String?
    ): ResponseEntity<MediaUploadResponse> {
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
        val resource = mediaService.getResource(actorId, mediaId)
        return ResponseEntity.ok(resource)
    }
}
