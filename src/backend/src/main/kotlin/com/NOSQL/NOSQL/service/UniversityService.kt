package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.UniversityDocument
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.model.generated.UniversityCreateResponse
import com.NOSQL.NOSQL.model.generated.UniversitySearchItem
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class UniversityService(
    private val universityRepository: UniversityRepository,
    private val mongoTemplate: MongoTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(q: String, limit: Int): List<UniversitySearchItem> {
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            log.debug("University search: empty query, returning empty list")
            return emptyList()
        }
        val escaped = escapeRegex(trimmed)
        val pattern = ".*$escaped.*"
        val criteria = Criteria().orOperator(
            Criteria.where("name").regex(pattern, "i"),
            Criteria.where("shortName").regex(pattern, "i"),
            Criteria.where("oldNames").regex(pattern, "i")
        )
        val query = Query().addCriteria(criteria).limit(limit.coerceIn(1, 100))
        val docs = mongoTemplate.find(query, UniversityDocument::class.java)
        log.info("University search: q='{}', limit={}, found={}", trimmed, limit, docs.size)
        return docs.map { doc ->
            UniversitySearchItem(
                id = doc.id,
                name = doc.name,
                shortName = doc.shortName,
                oldNames = doc.oldNames
            )
        }
    }

    private fun escapeRegex(s: String): String {
        return s.replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("*", "\\*")
            .replace("+", "\\+")
            .replace("?", "\\?")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("|", "\\|")
    }

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
