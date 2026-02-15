package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.model.Actor
import com.NOSQL.NOSQL.repository.ActorRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/actors")
class ActorController(private val actorRepository: ActorRepository) {

    @GetMapping
    fun getAllActors(): List<Actor> {
        return actorRepository.findAll()
    }

    @PostMapping
    fun createActor(@RequestBody actor: Actor): Actor {
        return actorRepository.save(actor)
    }
}
