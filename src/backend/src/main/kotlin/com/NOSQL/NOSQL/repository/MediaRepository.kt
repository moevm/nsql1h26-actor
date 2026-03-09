package com.NOSQL.NOSQL.repository

import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.stereotype.Repository
import java.io.InputStream

@Repository
class MediaRepository(
    private val gridFsOperations: GridFsOperations
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun store(
        inputStream: InputStream,
        filename: String,
        contentType: String?,
        actorId: String,
        type: String,
        caption: String?
    ): String {
        log.debug("Storing file in GridFS: actorId={}, type={}, filename={}", actorId, type, filename)
        val metadata = org.bson.Document().apply {
            put("actorId", actorId)
            put("type", type)
            put("caption", caption ?: "")
        }
        val objectId = gridFsOperations.store(
            inputStream,
            filename,
            contentType,
            metadata
        )
        log.debug("File stored in GridFS: mediaId={}", objectId.toHexString())
        return objectId.toHexString()
    }

    fun findOne(actorId: String, mediaId: String): GridFsResource? {
        log.debug("findOne: actorId={}, mediaId={}", actorId, mediaId)
        val objectId = try {
            ObjectId(mediaId)
        } catch (_: Exception) {
            log.warn("Invalid mediaId format: {}", mediaId)
            return null
        }
        val query = Query.query(
            Criteria.where("_id").`is`(objectId).and("metadata.actorId").`is`(actorId)
        )
        val file = gridFsOperations.findOne(query) ?: return null
        return gridFsOperations.getResource(file)
    }
}
