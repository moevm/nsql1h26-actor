package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.ActorMediaType
import com.NOSQL.NOSQL.model.generated.MediaUploadResponse
import com.NOSQL.NOSQL.repository.MediaRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.InputStream

@Service
class MediaService(
    private val mediaRepository: MediaRepository,
    private val actorService: ActorService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun upload(
        actorId: String,
        inputStream: InputStream,
        filename: String,
        contentType: String?,
        type: ActorMediaType,
        caption: String?,
    ): MediaUploadResponse {
        log.info("Uploading media for actorId={}, type={}, filename={}", actorId, type, filename)
        if (!actorService.existsById(actorId)) {
            log.warn("Actor not found for media upload: actorId={}", actorId)
            return MediaUploadResponse(
                status = MediaUploadResponse.Status.failed,
                mediaId = null,
                errorCode = "ACTOR_NOT_FOUND",
            )
        }
        val (header, fullStream) = MediaUploadValidator.peekHeaderAndWrap(inputStream)
        val validationError = MediaUploadValidator.validate(type, filename, header)
        if (validationError != null) {
            log.warn(
                "Media validation failed: actorId={}, filename={}, type={}, errorCode={}",
                actorId,
                filename,
                type,
                validationError,
            )
            fullStream.close()
            return MediaUploadResponse(
                status = MediaUploadResponse.Status.failed,
                mediaId = null,
                errorCode = validationError,
            )
        }
        val mediaId =
            mediaRepository.store(
                inputStream = fullStream,
                filename = filename,
                contentType = contentType,
                actorId = actorId,
                type = type.value,
                caption = caption,
            )
        when (type) {
            ActorMediaType.photo -> actorService.updatePhotos(actorId, mediaId, caption)
            ActorMediaType.video -> actorService.updateVideos(actorId, mediaId, caption)
        }
        log.info("Media uploaded successfully: actorId={}, mediaId={}", actorId, mediaId)
        return MediaUploadResponse(
            status = MediaUploadResponse.Status.ok,
            mediaId = mediaId,
            errorCode = null,
        )
    }

    fun getResource(
        actorId: String,
        mediaId: String,
    ): Resource {
        log.info("getResource: actorId={}, mediaId={}", actorId, mediaId)
        return mediaRepository.findOne(actorId, mediaId)
            ?: run {
                log.warn("Media not found: actorId={}, mediaId={}", actorId, mediaId)
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")
            }
    }

    fun delete(
        actorId: String,
        mediaId: String,
    ) {
        log.info("delete media: actorId={}, mediaId={}", actorId, mediaId)
        if (!actorService.existsById(actorId)) {
            log.warn("Actor not found for media delete: actorId={}", actorId)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Actor not found")
        }
        if (mediaRepository.findOne(actorId, mediaId) == null) {
            log.warn("Media not found: actorId={}, mediaId={}", actorId, mediaId)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")
        }
        actorService.removeMediaReferences(actorId, mediaId)
        if (!mediaRepository.deleteOne(actorId, mediaId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")
        }
    }
}
