package com.NOSQL.NOSQL.catalog

import com.NOSQL.NOSQL.model.generated.CatalogSnapshot
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.AdminRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import com.NOSQL.NOSQL.service.CatalogService
import com.NOSQL.NOSQL.service.MediaService
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.Base64

/**
 * Import from real JSON fixtures on the classpath and export consistency.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CatalogFixtureFileTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_catalog_fixtures"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }

        private val jsonMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
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
    lateinit var mediaService: MediaService

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
    @DisplayName("Import catalog-empty.json — empty DB after import")
    fun importEmptyFixture() {
        val snapshot = readFixture("fixtures/catalog/catalog-empty.json")
        catalogService.importSnapshot(snapshot)

        assertThat(universityRepository.count()).isZero()
        assertThat(actorRepository.count()).isZero()
        assertThat(adminRepository.count()).isZero()
        assertThat(catalogService.exportSnapshot().media).isEmpty()
    }

    @Test
    @DisplayName("Import catalog-sample.json — data and media; export matches expected content")
    fun importSampleAndVerifyExport() {
        val expected = readFixture("fixtures/catalog/catalog-sample.json")
        catalogService.importSnapshot(expected)

        val uni = universityRepository.findAll().single()
        assertThat(uni.id).isEqualTo("507f1f77bcf86cd799439011")
        assertThat(uni.name).isEqualTo("Тестовый университет")
        assertThat(uni.shortName).isEqualTo("ТУ")
        assertThat(uni.oldNames).containsExactly("Старое имя")

        val actor = actorRepository.findAll().single()
        assertThat(actor.id).isEqualTo("507f1f77bcf86cd799439012")
        assertThat(actor.firstName).isEqualTo("Анна")
        assertThat(actor.lastName).isEqualTo("Экспорт")
        assertThat(actor.photos?.single()?.id).isEqualTo("507f1f77bcf86cd799439014")

        val admin = adminRepository.findAll().single()
        assertThat(admin.email).isEqualTo("fixture-admin@example.com")

        val mediaId = "507f1f77bcf86cd799439014"
        val resource = mediaService.getResource(actorId = actor.id!!, mediaId = mediaId)
        val actualBytes = resource.inputStream.use { it.readAllBytes() }
        val expectedBytes = Base64.getDecoder().decode(
            expected.media.single { it.id == mediaId }.dataBase64
        )
        assertThat(actualBytes).isEqualTo(expectedBytes)
        assertThat(String(actualBytes)).isEqualTo("fixture-bytes")

        val exported = catalogService.exportSnapshot()
        assertThat(exported.universities.map { it.id to it.name })
            .containsExactly("507f1f77bcf86cd799439011" to "Тестовый университет")
        assertThat(exported.actors.single().firstName).isEqualTo("Анна")
        assertThat(exported.admins.single().email).isEqualTo("fixture-admin@example.com")
        assertThat(exported.media).hasSize(1)
        assertThat(exported.media.single().dataBase64).isEqualTo(expected.media.single().dataBase64)

        val exportedJson = jsonMapper.writeValueAsString(exported)
        val parsedAgain = jsonMapper.readValue(exportedJson, CatalogSnapshot::class.java)
        catalogService.importSnapshot(parsedAgain)
        assertThat(catalogService.exportSnapshot().actors.single().lastName).isEqualTo("Экспорт")
    }

    private fun readFixture(path: String): CatalogSnapshot {
        val resource = ClassPathResource(path)
        assertThat(resource.exists()).withFailMessage("Classpath resource missing: $path").isTrue()
        return resource.inputStream.use { input ->
            jsonMapper.readValue(input, CatalogSnapshot::class.java)
        }
    }
}
