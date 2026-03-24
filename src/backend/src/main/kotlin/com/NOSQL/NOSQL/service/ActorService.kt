package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.mapping.MappingFromApi
import com.NOSQL.NOSQL.mapping.MappingToApi
import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.generated.Actor
import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorCreateResponse
import com.NOSQL.NOSQL.model.generated.ActorUpdate
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.model.generated.UniversityInfo
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.MediaRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.bson.Document
import java.time.Instant
import java.time.OffsetDateTime

@Service
class ActorService(
    private val actorRepository: ActorRepository,
    private val mongoTemplate: MongoTemplate,
    private val universityRepository: UniversityRepository,
    private val mediaRepository: MediaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun countTotal(): Long {
        val count = actorRepository.count()
        log.debug("countTotal: $count")
        return count
    }

    fun create(actorCreate: ActorCreate): ActorCreateResponse {
        log.info("Creating actor: {} {}", actorCreate.firstName, actorCreate.lastName)
        actorCreate.education?.forEach { item ->
            if (!universityRepository.existsById(item.uniId)) {
                log.warn("University not found on actor create: uniId={}", item.uniId)
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "University not found: ${item.uniId}")
            }
        }
        val now = OffsetDateTime.now()
        val doc = MappingFromApi.actorCreateToDocument(actorCreate, now, now)
        val saved = actorRepository.save(doc)
        log.info("Actor created with id={}", saved.id)
        return ActorCreateResponse(
            status = ActorCreateResponse.Status.ok,
            id = saved.id,
            errorCode = null
        )
    }

    fun update(id: String, update: ActorUpdate): Actor {
        log.info("Updating actor id={}", id)
        if (!hasAnyUpdateField(update)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one field to update")
        }
        update.education?.forEach { item ->
            item.uniId?.let { uniId ->
                if (!universityRepository.existsById(uniId)) {
                    log.warn("University not found on actor update: uniId={}", uniId)
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "University not found: $uniId")
                }
            }
        }
        val doc = actorRepository.findById(id)
            .orElseThrow {
                log.warn("Actor not found for update: {}", id)
                ResponseStatusException(HttpStatus.NOT_FOUND, "Actor not found")
            }
        val merged = MappingFromApi.mergeActorDocument(doc, update)
        validateMainPhoto(merged)
        val saved = actorRepository.save(merged.copy(updatedAt = Instant.now()))
        log.info("Actor updated id={}", saved.id)
        return enrichWithUniversities(MappingToApi.documentToActor(saved))
    }

    private fun hasAnyUpdateField(u: ActorUpdate): Boolean =
        u.firstName != null || u.lastName != null || u.middleName != null ||
            u.birthDate != null || u.height != null || u.weight != null ||
            u.gender != null || u.hairColor != null || u.eyeColor != null ||
            u.bio != null || u.title != null || u.phone != null || u.email != null ||
            u.links != null || u.education != null || u.films != null ||
            u.theatrePlayItems != null || u.genres != null || u.mainPhotoId != null

    private fun validateMainPhoto(doc: ActorDocument) {
        val mainId = doc.mainPhotoId ?: return
        val photoIds = doc.photos?.mapNotNull { it.id }?.toSet() ?: emptySet()
        if (mainId !in photoIds) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "mainPhotoId must reference an existing photo id from photos"
            )
        }
    }

    fun getById(id: String): Actor {
        log.info("getById: id={}", id)
        val doc = actorRepository.findById(id)
            .orElseThrow {
                log.warn("Actor not found: {}", id)
                ResponseStatusException(HttpStatus.NOT_FOUND, "Actor not found")
            }
        return enrichWithUniversities(MappingToApi.documentToActor(doc))
    }

    fun findAll(
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
    ): List<Actor> {
        log.info("findAll: limit={}, offset={}, gender={}, theatre={}, universityId={}, name={}", limit, offset, gender, theatre, universityId, name)
        val query = Query()
        val criteria = mutableListOf<Criteria>()

        name?.trim()?.takeIf { it.isNotEmpty() }?.let { n ->
            val escaped = Regex.escape(n)
            val pattern = ".*$escaped.*"
            val fullNameOrder1 = Document(
                "\$trim", Document(
                    "input", Document(
                        "\$concat", listOf(
                            Document("\$ifNull", listOf("\$firstName", "")),
                            " ",
                            Document("\$ifNull", listOf("\$lastName", "")),
                            " ",
                            Document("\$ifNull", listOf("\$middleName", ""))
                        )
                    )
                )
            )
            val fullNameOrder2 = Document(
                "\$trim", Document(
                    "input", Document(
                        "\$concat", listOf(
                            Document("\$ifNull", listOf("\$lastName", "")),
                            " ",
                            Document("\$ifNull", listOf("\$firstName", "")),
                            " ",
                            Document("\$ifNull", listOf("\$middleName", ""))
                        )
                    )
                )
            )
            val fullNameExpr = Document("\$concat", listOf(fullNameOrder1, " ", fullNameOrder2))
            val regexMatchDoc = Document(
                "input", fullNameExpr
            ).append("regex", pattern).append("options", "i")
            val exprField = "\$expr"
            criteria.add(Criteria.where(exprField).`is`(Document("\$regexMatch", regexMatchDoc)))
        }

        gender?.let { criteria.add(Criteria.where("gender").`is`(it)) }
        weightMin?.let { criteria.add(Criteria.where("weight").gte(it)) }
        weightMax?.let { criteria.add(Criteria.where("weight").lte(it)) }
        heightMin?.let { criteria.add(Criteria.where("height").gte(it)) }
        heightMax?.let { criteria.add(Criteria.where("height").lte(it)) }
        hairColor?.let { criteria.add(Criteria.where("hairColor").`is`(it)) }
        eyeColor?.let { criteria.add(Criteria.where("eyeColor").`is`(it)) }
        title?.let { criteria.add(Criteria.where("title").`is`(it)) }
        theatre?.let { criteria.add(Criteria.where("theatrePlayItems.name").regex(it, "i")) }
        universityId?.let { criteria.add(Criteria.where("education.uniId").`is`(it)) }

        if (ageFrom != null || ageTo != null) {
            val fromDate = ageTo?.let { java.time.LocalDate.now().minusYears(it.toLong()) }
            val toDate = ageFrom?.let { java.time.LocalDate.now().minusYears(it.toLong()) }
            if (fromDate != null) criteria.add(Criteria.where("birthDate").gte(fromDate))
            if (toDate != null) criteria.add(Criteria.where("birthDate").lte(toDate))
        }

        if (activityYearFrom != null || activityYearTo != null) {
            val from = activityYearFrom ?: Int.MIN_VALUE
            val to = activityYearTo ?: Int.MAX_VALUE
            val yearInRange = Criteria.where("year").gte(from).lte(to)
            val yearCriteria = Criteria().orOperator(
                Criteria.where("films").elemMatch(yearInRange),
                Criteria.where("theatrePlayItems").elemMatch(Criteria.where("plays").elemMatch(yearInRange))
            )
            criteria.add(yearCriteria)
        }

        if (!genres.isNullOrEmpty()) {
            criteria.add(Criteria.where("genres").all(genres))
        }

        if (criteria.isNotEmpty()) {
            query.addCriteria(Criteria().andOperator(criteria))
        }
        query.with(Sort.by(Sort.Direction.ASC, "lastName", "firstName"))
        query.skip(offset.toLong()).limit(limit)
        val docs = mongoTemplate.find(query, ActorDocument::class.java)
        log.info("findAll returned {} actors", docs.size)
        return docs.map { MappingToApi.documentToActor(it) }.map(::enrichWithUniversities)
    }

    private fun enrichWithUniversities(actor: Actor): Actor {
        val education = actor.education?.map { item ->
            val uni = item.uniId?.let { universityRepository.findById(it).orElse(null) }
            item.copy(university = uni?.let { UniversityInfo(it.name, it.shortName, it.oldNames) })
        }
        return actor.copy(education = education)
    }

    fun updatePhotos(actorId: String, photoId: String, caption: String?) {
        log.debug("Adding photo to actor: actorId={}, photoId={}", actorId, photoId)
        val actor = actorRepository.findById(actorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Actor not found") }
        val photos = (actor.photos ?: emptyList()).toMutableList()
        photos.add(com.NOSQL.NOSQL.model.domain.PhotoItem(id = photoId, caption = caption))
        actorRepository.save(actor.copy(photos = photos, updatedAt = Instant.now()))
    }

    fun updateVideos(actorId: String, videoId: String, caption: String?) {
        log.debug("Adding video to actor: actorId={}, videoId={}", actorId, videoId)
        val actor = actorRepository.findById(actorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Actor not found") }
        val videos = (actor.videos ?: emptyList()).toMutableList()
        videos.add(com.NOSQL.NOSQL.model.domain.VideoItem(id = videoId, caption = caption))
        actorRepository.save(actor.copy(videos = videos, updatedAt = Instant.now()))
    }

    fun existsById(id: String): Boolean = actorRepository.existsById(id)

    fun deleteById(id: String) {
        log.info("Deleting actor: id={}", id)
        if (!actorRepository.existsById(id)) {
            log.warn("Actor not found for delete: {}", id)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Actor not found")
        }
        mediaRepository.deleteByActorId(id)
        actorRepository.deleteById(id)
        log.info("Actor deleted: id={}", id)
    }
}
