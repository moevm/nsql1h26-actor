package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.api.ActorsApi
import com.NOSQL.NOSQL.model.Actor as ActorEntity
import com.NOSQL.NOSQL.model.generated.Actor
import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.Actor.Role as ActorRole
import com.NOSQL.NOSQL.repository.ActorRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneOffset

@RestController
class ActorController(
    private val actorRepository: ActorRepository
) : ActorsApi {

    override fun v1ActorsGet(): ResponseEntity<List<Actor>> {
        val actors = actorRepository.findAll().map { it.toApiModel() }
        return ResponseEntity.ok(actors)
    }

    override fun v1ActorsIdGet(id: String): ResponseEntity<Actor> {
        return actorRepository.findById(id)
            .map { ResponseEntity.ok(it.toApiModel()) }
            .orElse(ResponseEntity.notFound().build())
    }

    override fun v1ActorsPost(actorCreate: ActorCreate): ResponseEntity<Actor> {
        val entity = actorCreate.toEntity()
        val saved = actorRepository.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toApiModel())
    }

    private fun ActorEntity.toApiModel() = Actor(
        id = id,
        name = name,
        email = email,
        role = enumValues<ActorRole>().find { it.value == role } ?: ActorRole.uSER,
        createdAt = createdAt?.atOffset(ZoneOffset.UTC)
    )

    private fun ActorCreate.toEntity() = ActorEntity(
        name = name,
        email = email,
        role = role?.value ?: "USER",
        createdAt = LocalDateTime.now()
    )
}
