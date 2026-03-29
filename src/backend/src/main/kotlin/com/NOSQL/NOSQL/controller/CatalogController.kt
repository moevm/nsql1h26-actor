package com.NOSQL.NOSQL.controller

import com.NOSQL.NOSQL.model.catalog.CatalogSnapshot
import com.NOSQL.NOSQL.service.CatalogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/catalog")
@Tag(name = "catalog", description = "Bulk catalog export/import (JSON)")
@SecurityRequirement(name = "bearerAuth")
class CatalogController(
    private val catalogService: CatalogService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/export", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Export full database to JSON (universities, actors, admins, media as Base64)")
    fun export(): CatalogSnapshot {
        log.info("GET /v1/catalog/export")
        return catalogService.exportSnapshot()
    }

    @PostMapping("/import", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Import: replace entire database contents from file")
    fun import(@RequestBody body: CatalogSnapshot): ResponseEntity<Unit> {
        log.info("POST /v1/catalog/import")
        catalogService.importSnapshot(body)
        return ResponseEntity.noContent().build()
    }
}
