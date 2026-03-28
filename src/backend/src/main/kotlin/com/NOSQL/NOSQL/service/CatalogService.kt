package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.catalog.CATALOG_FORMAT
import com.NOSQL.NOSQL.model.catalog.CATALOG_VERSION
import com.NOSQL.NOSQL.model.catalog.CatalogMediaEntry
import com.NOSQL.NOSQL.model.catalog.CatalogSnapshot
import com.NOSQL.NOSQL.repository.AdminRepository
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.MediaRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64

@Service
class CatalogService(
    private val universityRepository: UniversityRepository,
    private val actorRepository: ActorRepository,
    private val adminRepository: AdminRepository,
    private val mediaRepository: MediaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun exportSnapshot(): CatalogSnapshot {
        val media = mediaRepository.exportAllForBackup().map { row ->
            CatalogMediaEntry(
                id = row.id,
                actorId = row.actorId,
                filename = row.filename,
                contentType = row.contentType,
                type = row.mediaType,
                caption = row.caption,
                dataBase64 = Base64.getEncoder().encodeToString(row.bytes),
            )
        }
        return CatalogSnapshot(
            format = CATALOG_FORMAT,
            version = CATALOG_VERSION,
            universities = universityRepository.findAll(),
            actors = actorRepository.findAll(),
            admins = adminRepository.findAll(),
            media = media,
        )
    }

    fun importSnapshot(snapshot: CatalogSnapshot) {
        if (snapshot.format != CATALOG_FORMAT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: ${snapshot.format}")
        }
        if (snapshot.version != CATALOG_VERSION) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported version: ${snapshot.version}")
        }
        log.info(
            "Import snapshot: universities={}, actors={}, admins={}, media={}",
            snapshot.universities.size,
            snapshot.actors.size,
            snapshot.admins.size,
            snapshot.media.size
        )

        actorRepository.deleteAll()
        universityRepository.deleteAll()
        adminRepository.deleteAll()
        mediaRepository.deleteAll()

        snapshot.universities.forEach { universityRepository.save(it) }
        snapshot.actors.forEach { actorRepository.save(it) }
        snapshot.admins.forEach { adminRepository.save(it) }
        snapshot.media.forEach { entry ->
            try {
                val bytes = Base64.getDecoder().decode(entry.dataBase64)
                mediaRepository.storeWithId(
                    idHex = entry.id,
                    bytes = bytes,
                    filename = entry.filename,
                    contentType = entry.contentType,
                    actorId = entry.actorId,
                    type = entry.type,
                    caption = entry.caption,
                )
            } catch (e: IllegalArgumentException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media payload: ${e.message}")
            }
        }
    }
}
