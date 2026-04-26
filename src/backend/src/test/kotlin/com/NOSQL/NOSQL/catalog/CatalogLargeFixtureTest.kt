package com.NOSQL.NOSQL.catalog

import com.NOSQL.NOSQL.model.generated.CatalogSnapshot
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.AdminRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import com.NOSQL.NOSQL.service.CatalogService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Large snapshot lives in the repo: [fixtures/catalog/catalog-large.json] (no generators).
 * If you replace the file, update the size constants below.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CatalogLargeFixtureTest {
    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_catalog_large"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }

        private val jsonMapper =
            ObjectMapper()
                .registerKotlinModule()
                .registerModule(JavaTimeModule())

        /** Must match catalog-large.json contents */
        const val LARGE_UNIVERSITIES = 200
        const val LARGE_ACTORS = 500
        const val LARGE_ADMINS = 40
        const val LARGE_MEDIA = 500
    }

    @Autowired
    lateinit var catalogService: CatalogService

    @Autowired
    lateinit var universityRepository: UniversityRepository

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var adminRepository: AdminRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun clean() {
        mongoTemplate.db.getCollection("media.chunks").deleteMany(Document())
        mongoTemplate.db.getCollection("media.files").deleteMany(Document())
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        adminRepository.deleteAll()
    }

    @Test
    @DisplayName("Import catalog-large.json then export: snapshot equals original (full recursive compare)")
    fun importThenExport_equalsOriginalSnapshot() {
        val resource = ClassPathResource("fixtures/catalog/catalog-large.json")
        assertThat(resource.exists()).isTrue()
        val snapshot = resource.inputStream.use { jsonMapper.readValue(it, CatalogSnapshot::class.java) }

        assertThat(snapshot.universities).hasSize(LARGE_UNIVERSITIES)
        assertThat(snapshot.actors).hasSize(LARGE_ACTORS)
        assertThat(snapshot.admins).hasSize(LARGE_ADMINS)
        assertThat(snapshot.media).hasSize(LARGE_MEDIA)

        catalogService.importSnapshot(snapshot)

        assertThat(universityRepository.count()).isEqualTo(LARGE_UNIVERSITIES.toLong())
        assertThat(actorRepository.count()).isEqualTo(LARGE_ACTORS.toLong())
        assertThat(adminRepository.count()).isEqualTo(LARGE_ADMINS.toLong())

        val exported = catalogService.exportSnapshot()

        assertThat(exported)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(snapshot)

        val exportedJson = jsonMapper.writeValueAsString(exported)
        val parsedAgain = jsonMapper.readValue(exportedJson, CatalogSnapshot::class.java)
        assertThat(parsedAgain)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(snapshot)
    }
}
