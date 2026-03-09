package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorMediaType
import com.NOSQL.NOSQL.model.generated.MediaUploadResponse
import com.NOSQL.NOSQL.repository.ActorRepository
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
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
import java.io.ByteArrayInputStream

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class MediaServiceTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_media"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }
    }

    @Autowired
    lateinit var mediaService: MediaService

    @Autowired
    lateinit var actorService: ActorService

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    private lateinit var actorId: String

    @BeforeEach
    fun setUp() {
        mongoTemplate.db.getCollection("media.chunks").deleteMany(Document())
        mongoTemplate.db.getCollection("media.files").deleteMany(Document())
        actorRepository.deleteAll()
        actorId = actorService.create(ActorCreate(firstName = "Иван", lastName = "Петров")).id!!
    }

    @Nested
    @DisplayName("upload")
    inner class Upload {
        @Test
        fun `фото — success, mediaId в ответе, фото в документе актёра`() {
            val data = "fake image bytes".toByteArray()
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(data),
                filename = "photo.jpg",
                contentType = "image/jpeg",
                type = ActorMediaType.photo,
                caption = "Портрет"
            )
            assertThat(res.status).isEqualTo(MediaUploadResponse.Status.ok)
            assertThat(res.mediaId).isNotNull()
            assertThat(res.errorCode).isNull()
            val actor = actorRepository.findById(actorId).get()
            assertThat(actor.photos).hasSize(1)
            assertThat(actor.photos!![0].id).isEqualTo(res.mediaId)
            assertThat(actor.photos!![0].caption).isEqualTo("Портрет")
        }

        @Test
        fun `видео — success`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream("video".toByteArray()),
                filename = "clip.mp4",
                contentType = "video/mp4",
                type = ActorMediaType.video,
                caption = "Интервью"
            )
            assertThat(res.status).isEqualTo(MediaUploadResponse.Status.ok)
            assertThat(res.mediaId).isNotNull()
            val actor = actorRepository.findById(actorId).get()
            assertThat(actor.videos).hasSize(1)
            assertThat(actor.videos!![0].id).isEqualTo(res.mediaId)
        }

        @Test
        fun `актёр не найден — failed и ACTOR_NOT_FOUND`() {
            val res = mediaService.upload(
                actorId = "000000000000000000000000",
                inputStream = ByteArrayInputStream("x".toByteArray()),
                filename = "x",
                contentType = null,
                type = ActorMediaType.photo,
                caption = null
            )
            assertThat(res.status).isEqualTo(MediaUploadResponse.Status.failed)
            assertThat(res.errorCode).isEqualTo("ACTOR_NOT_FOUND")
            assertThat(res.mediaId).isNull()
        }
    }

    @Nested
    @DisplayName("getResource")
    inner class GetResource {
        @Test
        fun `успех — возвращает Resource`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream("image data".toByteArray()),
                filename = "p.jpg",
                contentType = "image/jpeg",
                type = ActorMediaType.photo,
                caption = null
            )
            val resource = mediaService.getResource(actorId, res.mediaId!!)
            assertThat(resource.exists()).isTrue()
            assertThat(resource.contentLength()).isEqualTo(10L)
        }

        @Test
        fun `медиа не найдено — 404`() {
            val ex = assertThrows<ResponseStatusException> {
                mediaService.getResource(actorId, "000000000000000000000000")
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(ex.reason).contains("Медиа не найдено")
        }

        @Test
        fun `неверный actorId для существующего mediaId — 404`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream("x".toByteArray()),
                filename = "x",
                contentType = null,
                type = ActorMediaType.photo,
                caption = null
            )
            val ex = assertThrows<ResponseStatusException> {
                mediaService.getResource("000000000000000000000000", res.mediaId!!)
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
