package com.NOSQL.NOSQL.repository

import com.NOSQL.NOSQL.model.domain.GridFsBackupRow
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.data.mongodb.gridfs.GridFsUpload
import org.springframework.stereotype.Repository
import java.io.ByteArrayInputStream
import java.io.InputStream

@Repository
class MediaRepository(
    private val gridFs: GridFsTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun store(
        inputStream: InputStream,
        filename: String,
        contentType: String?,
        actorId: String,
        type: String,
        caption: String?,
    ): String {
        log.debug("Storing file in GridFS: actorId={}, type={}, filename={}", actorId, type, filename)
        val metadata =
            org.bson.Document().apply {
                put("actorId", actorId)
                put("type", type)
                put("caption", caption ?: "")
                put("contentType", contentType ?: "application/octet-stream")
            }
        val objectId =
            gridFs.store(
                inputStream,
                filename,
                contentType,
                metadata,
            )
        log.debug("File stored in GridFS: mediaId={}", objectId.toHexString())
        return objectId.toHexString()
    }

    fun deleteAll() {
        log.debug("Deleting all GridFS files")
        gridFs.delete(Query())
    }

    fun exportAllForBackup(): List<GridFsBackupRow> {
        val out = mutableListOf<GridFsBackupRow>()
        val it = gridFs.find(Query()).iterator()
        while (it.hasNext()) {
            val file = it.next()
            val resource = gridFs.getResource(file)
            val bytes = resource.inputStream.use { s -> s.readAllBytes() }
            val meta = file.metadata ?: org.bson.Document()
            val actorId = meta.getString("actorId") ?: ""
            val mediaType = meta.getString("type") ?: "photo"
            val caption = meta.getString("caption")
            val id = file.objectId.toHexString()
            val filename = file.filename ?: "file"
            val contentType =
                meta.getString("contentType")
                    ?: runCatching { resource.contentType?.toString() }.getOrNull()
            out.add(
                GridFsBackupRow(
                    id = id,
                    actorId = actorId,
                    filename = filename,
                    contentType = contentType,
                    mediaType = mediaType,
                    caption = caption,
                    bytes = bytes,
                ),
            )
        }
        return out
    }

    fun storeWithId(
        idHex: String,
        bytes: ByteArray,
        filename: String,
        contentType: String?,
        actorId: String,
        type: String,
        caption: String?,
    ) {
        val id = ObjectId(idHex)
        val ct = contentType ?: "application/octet-stream"
        val metadata =
            org.bson.Document().apply {
                put("actorId", actorId)
                put("type", type)
                put("caption", caption ?: "")
                put("contentType", ct)
            }
        val upload =
            GridFsUpload
                .fromStream(ByteArrayInputStream(bytes))
                .id(id)
                .filename(filename)
                .contentType(ct)
                .metadata(metadata)
                .build()
        gridFs.store(upload)
        log.debug("Stored GridFS with id={}", idHex)
    }

    fun deleteByActorId(actorId: String) {
        log.debug("Deleting media for actorId={}", actorId)
        val query = Query.query(Criteria.where("metadata.actorId").`is`(actorId))
        gridFs.delete(query)
        log.debug("Media deleted for actorId={}", actorId)
    }

    fun findOne(
        actorId: String,
        mediaId: String,
    ): GridFsResource? {
        log.debug("findOne: actorId={}, mediaId={}", actorId, mediaId)
        val objectId =
            try {
                ObjectId(mediaId)
            } catch (_: Exception) {
                log.warn("Invalid mediaId format: {}", mediaId)
                return null
            }
        val query =
            Query.query(
                Criteria
                    .where("_id")
                    .`is`(objectId)
                    .and("metadata.actorId")
                    .`is`(actorId),
            )
        val file = gridFs.findOne(query) ?: return null
        return gridFs.getResource(file)
    }

    fun deleteOne(
        actorId: String,
        mediaId: String,
    ): Boolean {
        val objectId =
            try {
                ObjectId(mediaId)
            } catch (_: Exception) {
                log.warn("Invalid mediaId format: {}", mediaId)
                return false
            }
        val query =
            Query.query(
                Criteria
                    .where("_id")
                    .`is`(objectId)
                    .and("metadata.actorId")
                    .`is`(actorId),
            )
        if (gridFs.findOne(query) == null) return false
        gridFs.delete(query)
        log.debug("Deleted GridFS file: actorId={}, mediaId={}", actorId, mediaId)
        return true
    }
}
