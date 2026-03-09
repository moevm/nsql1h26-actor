package com.NOSQL.NOSQL.api

import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ApiE2ETest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_e2e"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }
    }

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var universityRepository: UniversityRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    private lateinit var mockMvc: MockMvc
    private val mapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }

    @BeforeEach
    fun setUp() {
        mongoTemplate.db.getCollection("media.chunks").deleteMany(Document())
        mongoTemplate.db.getCollection("media.files").deleteMany(Document())
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    @DisplayName("Полный сценарий только через эндпоинты: вуз → актёр → поиск → по id → загрузка медиа → получение медиа")
    fun fullFlowOnlyEndpoints() {
        // 1) Создать вуз — дергаем POST /v1/universities
        val createUniBody = """{"name":"ГИТИС","shortName":"ГИТИС","oldNames":["ГИТИС"]}"""
        val uniRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUniBody)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()
        val uniJson = mapper.readTree(uniRes.response.contentAsString)
        val uniId = uniJson["id"].asText()

        // 2) Создать актёра с этим вузом — POST /v1/actors
        val createActorBody = """
            {"firstName":"Иван","lastName":"Петров","birthDate":"1990-01-15","gender":"male","title":"national",
             "education":[{"uniId":"$uniId","graduationYear":2012,"name":"Актёр"}],
             "films":[{"title":"Фильм","year":2020,"role":"Роль","director":"Реж"}],
             "genres":["драма"]}
        """.trimIndent()
        val actorRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/actors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createActorBody)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()
        val actorId = mapper.readTree(actorRes.response.contentAsString)["id"].asText()

        // 3) Поиск по фильтрам — GET /v1/actors
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/actors")
                .param("gender", "male")
                .param("limit", "10")
                .param("offset", "0")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(actorId))
            .andExpect(jsonPath("$[0].firstName").value("Иван"))
            .andExpect(jsonPath("$[0].education[0].university.name").value("ГИТИС"))

        // 4) Получить актёра по id с обогащённым education — GET /v1/actors/{id}
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/actors/$actorId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(actorId))
            .andExpect(jsonPath("$.lastName").value("Петров"))
            .andExpect(jsonPath("$.education[0].university.shortName").value("ГИТИС"))
            .andExpect(jsonPath("$.films[0].title").value("Фильм"))

        // 5) Загрузить фото — POST /v1/actors/{id}/media
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", "image bytes".toByteArray())
        val uploadRes = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/v1/actors/$actorId/media")
                .file(file)
                .param("type", "photo")
                .param("caption", "Портрет")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.mediaId").exists())
            .andReturn()
        val mediaId = mapper.readTree(uploadRes.response.contentAsString)["mediaId"].asText()

        // 6) Получить медиа по id — GET /v1/actors/{actorId}/media/{mediaId}
        val mediaResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/actors/$actorId/media/$mediaId")
        )
            .andExpect(status().isOk())
            .andReturn()
        assertThat(mediaResult.response.contentAsByteArray).isNotEmpty()
    }

    @Test
    @DisplayName("Только эндпоинты: 400 при несуществующем вузе, 404 по id и по медиа")
    fun errorScenariosOnlyEndpoints() {
        // 400 — создание актёра с несуществующим uniId
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/actors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"А","lastName":"Б","education":[{"uniId":"000000000000000000000000"}]}""")
        )
            .andExpect(status().isBadRequest())

        // 404 — актёр не найден
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/actors/000000000000000000000000")
        )
            .andExpect(status().isNotFound())

        // Создаём вуз и актёра для проверки 404 по медиа
        val uniRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Вуз"}""")
        ).andExpect(status().isCreated()).andReturn()
        val uniId = mapper.readTree(uniRes.response.contentAsString)["id"].asText()
        val actorRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/actors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Х","lastName":"У","education":[{"uniId":"$uniId"}]}""")
        ).andReturn()
        val actorId = mapper.readTree(actorRes.response.contentAsString)["id"].asText()

        // 404 — медиа не найдено
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/actors/$actorId/media/000000000000000000000000")
        )
            .andExpect(status().isNotFound())

        // 404 — загрузка медиа для несуществующего актёра
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
