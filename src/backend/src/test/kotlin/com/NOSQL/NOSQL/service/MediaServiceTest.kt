package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.MediaTestBytes
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
        actorId = actorService.create(ActorCreate(firstName = "John", lastName = "Doe")).id!!
    }

    @Nested
    @DisplayName("upload")
    inner class Upload {
        @Test
        fun `photo upload succeeds and persists on actor`() {
            val data = MediaTestBytes.JPEG
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(data),
                filename = "photo.jpg",
                contentType = "image/jpeg",
                type = ActorMediaType.photo,
                caption = "Portrait"
            )
            assertThat(res.status).isEqualTo(MediaUploadResponse.Status.ok)
            assertThat(res.mediaId).isNotNull()
            assertThat(res.errorCode).isNull()
            val actor = actorRepository.findById(actorId).get()
            assertThat(actor.photos).hasSize(1)
            assertThat(actor.photos!![0].id).isEqualTo(res.mediaId)
            assertThat(actor.photos!![0].caption).isEqualTo("Portrait")
        }

        @Test
        fun `video upload succeeds`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(MediaTestBytes.MP4),
                filename = "clip.mp4",
                contentType = "video/mp4",
                type = ActorMediaType.video,
                caption = "Interview"
            )
            assertThat(res.status).isEqualTo(MediaUploadResponse.Status.ok)
            assertThat(res.mediaId).isNotNull()
            val actor = actorRepository.findById(actorId).get()
            assertThat(actor.videos).hasSize(1)
            assertThat(actor.videos!![0].id).isEqualTo(res.mediaId)
        }

        @Test
        fun `actor not found returns ACTOR_NOT_FOUND`() {
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

        @Test
        fun `wrong extension for photo type returns INVALID_MEDIA_EXTENSION`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(MediaTestBytes.JPEG),
                filename = "x.mp4",
                contentType = "video/mp4",
                type = ActorMediaType.photo,
                caption = null
            )
            assertThat(res.status).isEqualTo(MediaUploadResponse.Status.failed)
            assertThat(res.errorCode).isEqualTo(MediaUploadValidator.INVALID_MEDIA_EXTENSION)
            assertThat(res.mediaId).isNull()
        }

        @Test
        fun `content mismatch with extension returns INVALID_MEDIA_SIGNATURE`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(MediaTestBytes.PNG),
                filename = "photo.jpg",
                contentType = "image/jpeg",
                type = ActorMediaType.photo,
                caption = null
            )
            assertThat(res.status).isEqualTo(MediaUploadResponse.Status.failed)
            assertThat(res.errorCode).isEqualTo(MediaUploadValidator.INVALID_MEDIA_SIGNATURE)
            assertThat(res.mediaId).isNull()
        }
    }

    @Nested
    @DisplayName("getResource")
    inner class GetResource {
        @Test
        fun `getResource returns existing resource`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(MediaTestBytes.JPEG),
                filename = "p.jpg",
                contentType = "image/jpeg",
                type = ActorMediaType.photo,
                caption = null
            )
            val resource = mediaService.getResource(actorId, res.mediaId!!)
            assertThat(resource.exists()).isTrue()
            assertThat(resource.contentLength()).isEqualTo(MediaTestBytes.JPEG.size.toLong())
        }

        @Test
        fun `getResource returns 404 when media missing`() {
            val ex = assertThrows<ResponseStatusException> {
                mediaService.getResource(actorId, "000000000000000000000000")
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(ex.reason).contains("Media not found")
        }

        @Test
        fun `getResource returns 404 when actorId does not match`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(MediaTestBytes.JPEG),
                filename = "x.jpg",
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

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        fun `delete removes GridFS file and actor references`() {
            val res = mediaService.upload(
                actorId = actorId,
                inputStream = ByteArrayInputStream(MediaTestBytes.JPEG),
                filename = "p.jpg",
                contentType = "image/jpeg",
                type = ActorMediaType.photo,
                caption = null
            )
            val mediaId = res.mediaId!!
            mediaService.delete(actorId, mediaId)
            assertThrows<ResponseStatusException> { mediaService.getResource(actorId, mediaId) }
                .also { assertThat(it.statusCode).isEqualTo(HttpStatus.NOT_FOUND) }
            assertThat(actorRepository.findById(actorId).get().photos).isNull()
        }

        @Test
        fun `delete returns 404 when actor not found`() {
            val ex = assertThrows<ResponseStatusException> {
                mediaService.delete("000000000000000000000000", "000000000000000000000000")
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(ex.reason).contains("Actor not found")
        }

        @Test
        fun `delete returns 404 when media not found`() {
            val ex = assertThrows<ResponseStatusException> {
                mediaService.delete(actorId, "000000000000000000000000")
            }
            assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(ex.reason).contains("Media not found")
        }
    }
}
