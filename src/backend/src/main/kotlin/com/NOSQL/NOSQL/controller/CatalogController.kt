package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.api.CatalogApi
import com.NOSQL.NOSQL.model.generated.CatalogSnapshot
import com.NOSQL.NOSQL.service.CatalogService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CatalogController(
    private val catalogService: CatalogService,
) : CatalogApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun v1CatalogExportGet(): ResponseEntity<CatalogSnapshot> {
        log.info("GET /v1/catalog/export")
        return ResponseEntity.ok(catalogService.exportSnapshot())
    }

    override fun v1CatalogImportPost(catalogSnapshot: CatalogSnapshot): ResponseEntity<Unit> {
        log.info("POST /v1/catalog/import")
        catalogService.importSnapshot(catalogSnapshot)
        return ResponseEntity.noContent().build()
    }
}
