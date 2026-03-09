package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.EducationItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.server.ResponseStatusException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ActorServiceTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/test_actor_service"
            }
        }
    }

    @Autowired
    lateinit var actorService: ActorService

    @Autowired
    lateinit var universityService: UniversityService

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var universityRepository: UniversityRepository

    private lateinit var validUniId: String

    @BeforeEach
    fun setUp() {
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        val res = universityService.create(UniversityCreate(name = "Вуз", shortName = "В", oldNames = null))
        validUniId = res.id!!
    }

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `успех — возвращает id и status ok`() {
            val create = ActorCreate(
                firstName = "Иван",
                lastName = "Петров",
                birthDate = LocalDate.of(1990, 1, 1),
                gender = Gender.male,
                education = listOf(EducationItem(uniId = validUniId, graduationYear = 2012, name = "Актёр"))
            )
            val response = actorService.create(create)
            assertThat(response.status).isEqualTo(com.NOSQL.NOSQL.model.generated.ActorCreateResponse.Status.ok)
            assertThat(response.id).isNotNull()
            assertThat(response.errorCode).isNull()
            assertThat(actorRepository.existsById(response.id!!)).isTrue()
        }

        @Test
        fun `несуществующий uniId — 400 Вуз не найден`() {
            val create = ActorCreate(
                firstName = "Иван",
                lastName = "Петров",
                education = listOf(EducationItem(uniId = "000000000000000000000000", graduationYear = 2012, name = "Актёр"))
            )
            val ex = assertThrows<ResponseStatusException> { actorService.create(create) }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(ex.reason).contains("Вуз не найден")
        }

        @Test
        fun `без education — успех`() {
            val create = ActorCreate(firstName = "Иван", lastName = "Петров")
            val response = actorService.create(create)
            assertThat(response.status).isEqualTo(com.NOSQL.NOSQL.model.generated.ActorCreateResponse.Status.ok)
            assertThat(response.id).isNotNull()
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `успех — возвращает актёра с обогащённым education university`() {
            val create = ActorCreate(
                firstName = "Иван",
                lastName = "Петров",
                education = listOf(EducationItem(uniId = validUniId, graduationYear = 2012, name = "Актёр"))
            )
            val id = actorService.create(create).id!!
            val actor = actorService.getById(id)
            assertThat(actor.id).isEqualTo(id)
            assertThat(actor.firstName).isEqualTo("Иван")
            assertThat(actor.lastName).isEqualTo("Петров")
            assertThat(actor.education).hasSize(1)
            assertThat(actor.education!![0].university).isNotNull
            assertThat(actor.education!![0].university!!.name).isEqualTo("Вуз")
            assertThat(actor.education!![0].university!!.shortName).isEqualTo("В")
        }

        @Test
        fun `несуществующий id — 404`() {
            val ex = assertThrows<ResponseStatusException> {
                actorService.getById("000000000000000000000000")
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(ex.reason).contains("Актёр не найден")
        }
    }

    @Nested
    @DisplayName("updatePhotos / updateVideos")
    inner class UpdateMedia {
        @Test
        fun `updatePhotos — добавляет фото в документ`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            actorService.updatePhotos(id, "photoId123", "Подпись")
            val actor = actorRepository.findById(id).get()
            assertThat(actor.photos).hasSize(1)
            assertThat(actor.photos!![0].id).isEqualTo("photoId123")
            assertThat(actor.photos!![0].caption).isEqualTo("Подпись")
        }

        @Test
        fun `updatePhotos — актёр не найден 404`() {
            val ex = assertThrows<ResponseStatusException> {
                actorService.updatePhotos("000000000000000000000000", "x", null)
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `updateVideos — добавляет видео`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            actorService.updateVideos(id, "videoId456", "Видео")
            val actor = actorRepository.findById(id).get()
            assertThat(actor.videos).hasSize(1)
            assertThat(actor.videos!![0].id).isEqualTo("videoId456")
        }

        @Test
        fun `updateVideos — актёр не найден 404`() {
            val ex = assertThrows<ResponseStatusException> {
                actorService.updateVideos("000000000000000000000000", "x", null)
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("existsById")
    inner class ExistsById {
        @Test
        fun `true когда актёр есть`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            assertThat(actorService.existsById(id)).isTrue()
        }

        @Test
        fun `false когда нет`() {
            assertThat(actorService.existsById("000000000000000000000000")).isFalse()
        }
    }
}
