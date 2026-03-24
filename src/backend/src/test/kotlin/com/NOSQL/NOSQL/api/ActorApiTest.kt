package com.NOSQL.NOSQL.api

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.EducationItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ActorApiTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_actor_api"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }
    }

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var universityRepository: UniversityRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
    private lateinit var uniId: String
    private lateinit var actorId: String

    @BeforeEach
    fun setUp() {
        mongoTemplate.db.getCollection("media.chunks").deleteMany(Document())
        mongoTemplate.db.getCollection("media.files").deleteMany(Document())
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        val uniRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Вуз","shortName":"В"}""")
        ).andReturn()
        val uniBody = objectMapper.readTree(uniRes.response.contentAsString)
        uniId = uniBody["id"].asText()
        val createBody = ActorCreate(
            firstName = "Иван",
            lastName = "Петров",
            birthDate = LocalDate.of(1990, 1, 1),
            gender = Gender.male,
            title = Title.national,
            education = listOf(EducationItem(uniId = uniId, graduationYear = 2012, name = "Актёр"))
        )
        val createRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/actors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody))
        ).andReturn()
        actorId = objectMapper.readTree(createRes.response.contentAsString)["id"].asText()
    }

    @Nested
    @DisplayName("POST /v1/actors")
    inner class PostActors {
        @Test
        fun `201 — создание актёра`() {
            val body = """{"firstName":"Мария","lastName":"Иванова"}"""
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.id").exists())
        }

        @Test
        fun `400 — несуществующий uniId`() {
            val body = """{"firstName":"Иван","lastName":"П","education":[{"uniId":"000000000000000000000000","graduationYear":2012}]}"""
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest())
        }
    }

    @Nested
    @DisplayName("GET /v1/actors")
    inner class GetActors {
        @Test
        fun `200 — список с фильтрами`() {
            mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/actors")
                    .param("gender", "male")
                    .param("limit", "10")
                    .param("offset", "0")
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Иван"))
        }
    }

    @Nested
    @DisplayName("GET /v1/actors/{id}")
    inner class GetActorById {
        @Test
        fun `200 — актёр с обогащённым education`() {
            mockMvc.perform(MockMvcRequestBuilders.get("/v1/actors/$actorId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(actorId))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.education[0].university.name").value("Вуз"))
        }

        @Test
        fun `404 — несуществующий id`() {
            mockMvc.perform(MockMvcRequestBuilders.get("/v1/actors/000000000000000000000000"))
                .andExpect(status().isNotFound())
        }
    }

    @Nested
    @DisplayName("POST /v1/actors/{id}/media")
    inner class PostMedia {
        @Test
        fun `201 — загрузка фото`() {
            val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", "image data".toByteArray())
            mockMvc.perform(
                MockMvcRequestBuilders.multipart("/v1/actors/$actorId/media")
                    .file(file)
                    .param("type", "photo")
                    .param("caption", "Портрет")
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.mediaId").exists())
        }

        @Test
        fun `404 — актёр не найден`() {
            val file = MockMultipartFile("file", "x", "image/jpeg", "x".toByteArray())
            mockMvc.perform(
                MockMvcRequestBuilders.multipart("/v1/actors/000000000000000000000000/media")
                    .file(file)
                    .param("type", "photo")
            )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACTOR_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("GET /v1/actors/{actorId}/media/{mediaId}")
    inner class GetMedia {
        @Test
        fun `200 — получение медиа`() {
            val file = MockMultipartFile("file", "p.jpg", "image/jpeg", "bytes".toByteArray())
            mockMvc.perform(
                MockMvcRequestBuilders.multipart("/v1/actors/$actorId/media")
                    .file(file)
                    .param("type", "photo")
            )
            val mediaRes = mockMvc.perform(MockMvcRequestBuilders.get("/v1/actors/$actorId"))
                .andReturn()
            val photos = objectMapper.readTree(mediaRes.response.contentAsString).get("photos")
            val mediaId = photos?.get(0)?.get("id")?.asText() ?: return
            val getRes = mockMvc.perform(MockMvcRequestBuilders.get("/v1/actors/$actorId/media/$mediaId"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=100"))
                .andReturn()
            assertThat(getRes.response.contentAsByteArray.size).isPositive()
        }

        @Test
        fun `404 — медиа не найдено`() {
            mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/actors/$actorId/media/000000000000000000000000")
            )
                .andExpect(status().isNotFound())
        }
    }
}
