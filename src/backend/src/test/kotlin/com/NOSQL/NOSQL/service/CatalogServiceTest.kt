package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.AdminDocument
import com.NOSQL.NOSQL.model.catalog.CATALOG_VERSION
import com.NOSQL.NOSQL.model.generated.CatalogSnapshot
import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorMediaType
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.AdminRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.web.server.ResponseStatusException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CatalogServiceTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_catalog"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }
    }

    @Autowired
    lateinit var catalogService: CatalogService

    @Autowired
    lateinit var actorService: ActorService

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

    @Nested
    @DisplayName("export")
    inner class Export {
        @Test
        fun `пустая БД — корректный снимок`() {
            val snap = catalogService.exportSnapshot()
            assertThat(snap.version).isEqualTo(CATALOG_VERSION)
            assertThat(snap.universities).isEmpty()
            assertThat(snap.actors).isEmpty()
            assertThat(snap.admins).isEmpty()
            assertThat(snap.media).isEmpty()
        }

        @Test
        fun `снимок содержит вуз актёра админа и медиа`() {
            universityRepository.save(
                com.NOSQL.NOSQL.model.UniversityDocument(
                    id = null,
                    name = "МГУ",
                    shortName = "МГУ",
                    oldNames = null
                )
            )
            val actorId = actorService.create(
                ActorCreate(
                    firstName = "Иван",
                    lastName = "Тестов",
                    birthDate = LocalDate.of(1990, 1, 1),
                    gender = Gender.male,
                )
            ).id!!
            adminRepository.save(
                AdminDocument(
                    id = null,
                    email = "a@b.c",
                    passwordHash = "hash",
                    createdAt = Instant.parse("2020-01-01T00:00:00Z")
                )
            )
            mediaService.upload(
                actorId = actorId,
                inputStream = "hello".byteInputStream(),
                filename = "f.jpg",
                contentType = "image/jpeg",
                type = ActorMediaType.photo,
                caption = "c"
            )

            val snap = catalogService.exportSnapshot()
            assertThat(snap.universities).hasSize(1)
            assertThat(snap.universities[0].name).isEqualTo("МГУ")
            assertThat(snap.actors).hasSize(1)
            assertThat(snap.actors[0].firstName).isEqualTo("Иван")
            assertThat(snap.admins).hasSize(1)
            assertThat(snap.media).hasSize(1)
            assertThat(snap.media[0].dataBase64).isNotBlank()
        }
    }

    @Nested
    @DisplayName("import и round-trip")
    inner class Import {
        @Test
        fun `импорт восстанавливает данные и медиа`() {
            universityRepository.save(
                com.NOSQL.NOSQL.model.UniversityDocument(
                    id = null,
                    name = "Вуз1",
                    shortName = "В1",
                    oldNames = listOf("старое")
                )
            )
            val actorId = actorService.create(
                ActorCreate(
                    firstName = "Пётр",
                    lastName = "Петров",
                    birthDate = LocalDate.of(1985, 5, 5),
                    gender = Gender.male,
                )
            ).id!!
            mediaService.upload(
                actorId = actorId,
                inputStream = "binary".byteInputStream(),
                filename = "x.png",
                contentType = "image/png",
                type = ActorMediaType.photo,
                caption = null
            )
            adminRepository.save(
                AdminDocument(
                    id = null,
                    email = "admin@test.ru",
                    passwordHash = "x",
                    createdAt = Instant.now()
                )
            )

            val exported = catalogService.exportSnapshot()

            actorRepository.deleteAll()
            universityRepository.deleteAll()
            adminRepository.deleteAll()
            mongoTemplate.db.getCollection("media.chunks").deleteMany(Document())
            mongoTemplate.db.getCollection("media.files").deleteMany(Document())

            catalogService.importSnapshot(exported)

            assertThat(universityRepository.count()).isEqualTo(1L)
            assertThat(universityRepository.findAll()[0].name).isEqualTo("Вуз1")
            assertThat(actorRepository.count()).isEqualTo(1L)
            assertThat(adminRepository.count()).isEqualTo(1L)
            val actor = actorRepository.findAll()[0]
            assertThat(actor.firstName).isEqualTo("Пётр")
            assertThat(actor.photos).hasSize(1)
            val mediaId = actor.photos!![0].id!!
            val resource = mediaService.getResource(actorId = actor.id!!, mediaId = mediaId)
            val bytes = resource.inputStream.use { it.readAllBytes() }
            assertThat(bytes).isEqualTo("binary".toByteArray())
        }

        @Test
        fun `неверная version — 400`() {
            val empty = catalogService.exportSnapshot()
            val ex = assertThrows<ResponseStatusException> {
                catalogService.importSnapshot(empty.copy(version = 999))
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
