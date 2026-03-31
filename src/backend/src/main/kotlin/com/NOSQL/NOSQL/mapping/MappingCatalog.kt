package com.NOSQL.NOSQL.mapping

import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.AdminDocument as DomainAdminDocument
import com.NOSQL.NOSQL.model.UniversityDocument
import com.NOSQL.NOSQL.model.catalog.CATALOG_FORMAT
import com.NOSQL.NOSQL.model.catalog.CATALOG_VERSION
import com.NOSQL.NOSQL.model.generated.AdminDocument as GeneratedAdminDocument
import com.NOSQL.NOSQL.model.generated.CatalogMediaEntry
import com.NOSQL.NOSQL.model.generated.CatalogSnapshot
import com.NOSQL.NOSQL.model.generated.UniversitySearchItem
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneOffset

object MappingCatalog {

    fun toCatalogSnapshot(
        universities: List<UniversityDocument>,
        actors: List<ActorDocument>,
        admins: List<DomainAdminDocument>,
        media: List<CatalogMediaEntry>,
    ): CatalogSnapshot =
        CatalogSnapshot(
            format = CATALOG_FORMAT,
            version = CATALOG_VERSION,
            universities = universities.map(::universityToSearchItem),
            actors = actors.map { MappingToApi.documentToActor(it) },
            admins = admins.map(::adminToGenerated),
            media = media,
        )

    private fun universityToSearchItem(doc: UniversityDocument): UniversitySearchItem =
        UniversitySearchItem(
            id = doc.id,
            name = doc.name,
            shortName = doc.shortName,
            oldNames = doc.oldNames,
        )

    private fun adminToGenerated(doc: DomainAdminDocument): GeneratedAdminDocument =
        GeneratedAdminDocument(
            id = doc.id,
            email = doc.email,
            passwordHash = doc.passwordHash,
            createdAt = doc.createdAt?.let { java.time.OffsetDateTime.ofInstant(it, ZoneOffset.UTC) },
        )

    fun universityFromSearchItem(item: UniversitySearchItem): UniversityDocument =
        UniversityDocument(
            id = item.id,
            name = item.name ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "University name required"),
            shortName = item.shortName,
            oldNames = item.oldNames,
        )

    fun adminFromGenerated(a: GeneratedAdminDocument): DomainAdminDocument =
        DomainAdminDocument(
            id = a.id,
            email = a.email ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin email required"),
            passwordHash = a.passwordHash
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin passwordHash required"),
            createdAt = a.createdAt?.toInstant(),
        )
}
