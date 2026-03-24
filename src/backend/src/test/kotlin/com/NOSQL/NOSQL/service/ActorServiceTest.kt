package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorUpdate
import com.NOSQL.NOSQL.model.generated.ContactLinkItem
import com.NOSQL.NOSQL.model.generated.EducationCreateItem
import com.NOSQL.NOSQL.model.generated.EducationItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.FilmPlayItem
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
import java.net.URI
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
            val db = "test_actor_service"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
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

    @Autowired
    lateinit var mediaService: MediaService

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
                phone = "+7 999 123-45-67",
                email = "ivan@example.com",
                links = listOf(
                    ContactLinkItem(name = "ВК", url = URI("https://vk.com/ivan")),
                    ContactLinkItem(name = "Рутуб", url = URI("https://rutube.ru/ivan"))
                ),
                education = listOf(EducationCreateItem(uniId = validUniId))
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
                education = listOf(EducationCreateItem(uniId = "000000000000000000000000"))
            )
            val ex = assertThrows<ResponseStatusException> { actorService.create(create) }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(ex.reason).contains("University not found")
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
    @DisplayName("update")
    inner class Update {
        private val samplePhotoId = "aaaaaaaaaaaaaaaaaaaaaaaa"

        @Test
        fun `успех — меняется только переданное поле, остальное сохраняется`() {
            val id = actorService.create(
                ActorCreate(
                    firstName = "Иван",
                    lastName = "Петров",
                    birthDate = LocalDate.of(1990, 1, 1),
                    gender = Gender.male,
                    title = Title.national,
                    phone = "+7 900 000-00-00",
                    education = listOf(EducationCreateItem(uniId = validUniId))
                )
            ).id!!
            val updated = actorService.update(id, ActorUpdate(firstName = "Пётр"))
            assertThat(updated.firstName).isEqualTo("Пётр")
            assertThat(updated.lastName).isEqualTo("Петров")
            assertThat(updated.birthDate).isEqualTo(LocalDate.of(1990, 1, 1))
            assertThat(updated.gender).isEqualTo(Gender.male)
            assertThat(updated.title).isEqualTo(Title.national)
            assertThat(updated.phone).isEqualTo("+7 900 000-00-00")
            assertThat(updated.education).hasSize(1)
            assertThat(updated.education!![0].university!!.name).isEqualTo("Вуз")
        }

        @Test
        fun `genres — при передаче списка он заменяется целиком, emptyList() очищает`() {
            val id = actorService.create(
                ActorCreate(firstName = "Иван", lastName = "Петров", genres = listOf("драма", "комедия"))
            ).id!!
            val withOne = actorService.update(id, ActorUpdate(genres = listOf("триллер")))
            assertThat(withOne.genres).containsExactly("триллер")
            val cleared = actorService.update(id, ActorUpdate(genres = emptyList()))
            assertThat(cleared.genres).isEmpty()
        }

        @Test
        fun `links — заменяются целиком`() {
            val id = actorService.create(
                ActorCreate(
                    firstName = "Иван",
                    lastName = "Петров",
                    links = listOf(
                        ContactLinkItem(name = "ВК", url = URI("https://vk.com/a")),
                        ContactLinkItem(name = "Сайт", url = URI("https://a.ru"))
                    )
                )
            ).id!!
            val replaced = actorService.update(
                id,
                ActorUpdate(links = listOf(ContactLinkItem(name = "Телеграм", url = URI("https://t.me/a"))))
            )
            assertThat(replaced.links).hasSize(1)
            assertThat(replaced.links!![0].name).isEqualTo("Телеграм")
        }

        @Test
        fun `films — заменяются целиком`() {
            val id = actorService.create(
                ActorCreate(
                    firstName = "Иван",
                    lastName = "Петров",
                    films = listOf(FilmPlayItem(title = "Старый", year = 2000, role = "роль", director = "реж"))
                )
            ).id!!
            val u = actorService.update(
                id,
                ActorUpdate(films = listOf(FilmPlayItem(title = "Новый", year = 2020, role = "главная", director = "др")))
            )
            assertThat(u.films).hasSize(1)
            assertThat(u.films!![0].title).isEqualTo("Новый")
            assertThat(u.films[0].year).isEqualTo(2020)
        }

        @Test
        fun `пустой ActorUpdate — 400`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            val ex = assertThrows<ResponseStatusException> { actorService.update(id, ActorUpdate()) }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(ex.reason).contains("at least one field")
        }

        @Test
        fun `несуществующий актёр — 404`() {
            val ex = assertThrows<ResponseStatusException> {
                actorService.update("000000000000000000000000", ActorUpdate(lastName = "X"))
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `education с несуществующим uniId — 400`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            val ex = assertThrows<ResponseStatusException> {
                actorService.update(
                    id,
                    ActorUpdate(education = listOf(EducationItem(uniId = "000000000000000000000000", graduationYear = 2010, name = "X")))
                )
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(ex.reason).contains("University not found")
        }

        @Test
        fun `mainPhotoId совпадает с id фото — успех`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            actorService.updatePhotos(id, samplePhotoId, "подпись")
            val actor = actorService.update(id, ActorUpdate(mainPhotoId = samplePhotoId))
            assertThat(actor.mainPhotoId).isEqualTo(samplePhotoId)
            assertThat(actor.photos?.map { it.id }).contains(samplePhotoId)
        }

        @Test
        fun `mainPhotoId не из списка photos — 400`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            actorService.updatePhotos(id, samplePhotoId, null)
            val ex = assertThrows<ResponseStatusException> {
                actorService.update(id, ActorUpdate(mainPhotoId = "bbbbbbbbbbbbbbbbbbbbbbbb"))
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(ex.reason).contains("mainPhotoId")
        }

        @Test
        fun `mainPhotoId без фото у актёра — 400`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            val ex = assertThrows<ResponseStatusException> {
                actorService.update(id, ActorUpdate(mainPhotoId = samplePhotoId))
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `успех — возвращает актёра с обогащённым education university и контактами`() {
            val create = ActorCreate(
                firstName = "Иван",
                lastName = "Петров",
                phone = "+7 999 111-22-33",
                email = "actor@mail.ru",
                links = listOf(
                    ContactLinkItem(name = "ВК", url = URI("https://vk.com/ivan_petrov")),
                    ContactLinkItem(name = "Личный сайт", url = URI("https://ivan-actor.ru"))
                ),
                education = listOf(EducationCreateItem(uniId = validUniId))
            )
            val id = actorService.create(create).id!!
            val actor = actorService.getById(id)
            assertThat(actor.id).isEqualTo(id)
            assertThat(actor.firstName).isEqualTo("Иван")
            assertThat(actor.lastName).isEqualTo("Петров")
            assertThat(actor.phone).isEqualTo("+7 999 111-22-33")
            assertThat(actor.email).isEqualTo("actor@mail.ru")
            assertThat(actor.links).hasSize(2)
            assertThat(actor.links!![0].name).isEqualTo("ВК")
            assertThat(actor.links!![0].url).isEqualTo(URI("https://vk.com/ivan_petrov"))
            assertThat(actor.links!![1].name).isEqualTo("Личный сайт")
            assertThat(actor.links!![1].url).isEqualTo(URI("https://ivan-actor.ru"))
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
            assertThat(ex.reason).contains("Actor not found")
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

    @Nested
    @DisplayName("deleteById")
    inner class DeleteById {
        @Test
        fun `успех — удаляет актёра из БД`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            assertThat(actorRepository.existsById(id)).isTrue()
            actorService.deleteById(id)
            assertThat(actorRepository.existsById(id)).isFalse()
        }

        @Test
        fun `успех — удаляет медиа актёра из GridFS`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            val file = org.springframework.mock.web.MockMultipartFile("file", "photo.jpg", "image/jpeg", "image bytes".toByteArray())
            val uploadRes = mediaService.upload(id, file.inputStream, "photo.jpg", "image/jpeg", com.NOSQL.NOSQL.model.generated.ActorMediaType.photo, null)
            val mediaId = uploadRes.mediaId!!
            assertThat(mediaService.getResource(id, mediaId).exists()).isTrue()
            actorService.deleteById(id)
            assertThat(actorRepository.existsById(id)).isFalse()
            val ex = assertThrows<ResponseStatusException> { mediaService.getResource(id, mediaId) }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `успех — удаляет все медиа (фото и видео) актёра`() {
            val id = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
            val photoFile = org.springframework.mock.web.MockMultipartFile("file", "photo.jpg", "image/jpeg", "photo".toByteArray())
            val videoFile = org.springframework.mock.web.MockMultipartFile("file", "video.mp4", "video/mp4", "video".toByteArray())
            val photoRes = mediaService.upload(id, photoFile.inputStream, "photo.jpg", "image/jpeg", com.NOSQL.NOSQL.model.generated.ActorMediaType.photo, null)
            val videoRes = mediaService.upload(id, videoFile.inputStream, "video.mp4", "video/mp4", com.NOSQL.NOSQL.model.generated.ActorMediaType.video, null)
            actorService.deleteById(id)
            assertThrows<ResponseStatusException> { mediaService.getResource(id, photoRes.mediaId!!) }
            assertThrows<ResponseStatusException> { mediaService.getResource(id, videoRes.mediaId!!) }
        }

        @Test
        fun `несуществующий id — 404`() {
            val ex = assertThrows<ResponseStatusException> {
                actorService.deleteById("000000000000000000000000")
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(ex.reason).contains("Actor not found")
        }
    }
}
