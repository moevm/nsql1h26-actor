package com.NOSQL.NOSQL.model.catalog

import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.AdminDocument
import com.NOSQL.NOSQL.model.UniversityDocument
import io.swagger.v3.oas.annotations.media.Schema

const val CATALOG_FORMAT = "nsql1-catalog"
const val CATALOG_VERSION = 1

@Schema(description = "Full DB snapshot: universities, actors, admins, GridFS files (base64). Import replaces all data.")
data class CatalogSnapshot(
    @Schema(example = CATALOG_FORMAT)
    val format: String = CATALOG_FORMAT,
    @Schema(example = "1")
    val version: Int = CATALOG_VERSION,
    val universities: List<UniversityDocument> = emptyList(),
    val actors: List<ActorDocument> = emptyList(),
    val admins: List<AdminDocument> = emptyList(),
    val media: List<CatalogMediaEntry> = emptyList(),
)

@Schema(description = "GridFS file included in the snapshot")
data class CatalogMediaEntry(
    val id: String,
    val actorId: String,
    val filename: String,
    val contentType: String? = null,
    @Schema(description = "photo or video")
    val type: String,
    val caption: String? = null,
    val dataBase64: String,
)
