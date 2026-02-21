package com.NOSQL.NOSQL.repository

import com.NOSQL.NOSQL.model.Actor
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ActorRepository : MongoRepository<Actor, String>
