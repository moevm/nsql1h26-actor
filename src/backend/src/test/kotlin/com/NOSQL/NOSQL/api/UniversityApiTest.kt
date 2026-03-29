package com.NOSQL.NOSQL.api

import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.AdminDocument
import com.NOSQL.NOSQL.model.domain.EducationItem
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.AdminRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.MockMvcConfigurer
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class UniversityApiTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_university_api"
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
    lateinit var universityRepository: UniversityRepository

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var adminRepository: AdminRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    private val mapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
    private val testAdminEmail = "uni-api@test.test"
    private val testAdminPassword = "testpass123"

    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        if (adminRepository.findByEmail(testAdminEmail) == null) {
            adminRepository.save(
                AdminDocument(
                    email = testAdminEmail,
                    passwordHash = passwordEncoder.encode(testAdminPassword)!!,
                    createdAt = Instant.now()
                )
            )
        }
        val securityConfigurer = SecurityMockMvcConfigurers.springSecurity() as MockMvcConfigurer
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(securityConfigurer)
            .build()
        token = getToken()
    }

    private fun getToken(): String {
        val loginRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$testAdminEmail","password":"$testAdminPassword"}""")
        ).andExpect(status().isOk()).andReturn()
        return mapper.readTree(loginRes.response.contentAsString)["token"].asText()
    }

    @Test
    @DisplayName("POST /v1/universities — 201, id and status ok")
    fun createUniversity() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"МГУ","shortName":"МГУ","oldNames":["Императорский"]}""")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.errorCode").isEmpty())
    }

    @Test
    @DisplayName("POST /v1/universities — name only")
    fun createUniversityMinimal() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"СПбГУ"}""")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    @DisplayName("GET /v1/universities/search — string search returns id and names")
    fun searchUniversities() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"ГИТИС","shortName":"ГИТИС","oldNames":["РАТИ"]}""")
        ).andExpect(status().isCreated())
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/universities/search").param("q", "ГИТИС").param("limit", "5")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").value("ГИТИС"))
            .andExpect(jsonPath("$[0].shortName").value("ГИТИС"))
            .andExpect(jsonPath("$[0].oldNames[0]").value("РАТИ"))
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/universities/search").param("q", "РАТИ")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("ГИТИС"))
    }

    @Test
    @DisplayName("PATCH /v1/universities/{id} — 200, updates name and shortName")
    fun patchUniversity() {
        val createRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Старое имя","shortName":"СИ"}""")
        ).andExpect(status().isCreated()).andReturn()
        val id = mapper.readTree(createRes.response.contentAsString)["id"].asText()

        mockMvc.perform(
            MockMvcRequestBuilders.patch("/v1/universities/$id")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Новое имя","shortName":"НИ"}""")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/universities/search").param("q", "Новое").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Новое имя"))
            .andExpect(jsonPath("$[0].shortName").value("НИ"))
    }

    @Test
    @DisplayName("PATCH /v1/universities/{id} — 400 on empty update body")
    fun patchUniversityEmptyBody() {
        val createRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Вуз"}""")
        ).andExpect(status().isCreated()).andReturn()
        val id = mapper.readTree(createRes.response.contentAsString)["id"].asText()

        mockMvc.perform(
            MockMvcRequestBuilders.patch("/v1/universities/$id")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest())
    }

    @Test
    @DisplayName("PATCH /v1/universities/{id} — 404")
    fun patchUniversityNotFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/v1/universities/000000000000000000000000")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"X"}""")
        )
            .andExpect(status().isNotFound())
    }

    @Test
    @DisplayName("DELETE /v1/universities/{id} — 204")
    fun deleteUniversity() {
        val createRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"На удаление"}""")
        ).andExpect(status().isCreated()).andReturn()
        val id = mapper.readTree(createRes.response.contentAsString)["id"].asText()

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/v1/universities/$id")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent())

        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/universities/search").param("q", "На удаление").param("limit", "5")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @DisplayName("DELETE /v1/universities/{id} — 401 without JWT")
    fun deleteUniversityUnauthorized() {
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/universities/000000000000000000000000"))
            .andExpect(status().isUnauthorized())
    }

    @Test
    @DisplayName("DELETE /v1/universities/{id} — 404")
    fun deleteUniversityNotFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/v1/universities/000000000000000000000000")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound())
    }

    @Test
    @DisplayName("DELETE /v1/universities/{id} — 409 when referenced by actor")
    fun deleteUniversityConflict() {
        val createRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Связанный вуз"}""")
        ).andExpect(status().isCreated()).andReturn()
        val uniId = mapper.readTree(createRes.response.contentAsString)["id"].asText()

        actorRepository.save(
            ActorDocument(
                firstName = "А",
                lastName = "Б",
                education = listOf(EducationItem(uniId = uniId, graduationYear = null, name = null)),
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/v1/universities/$uniId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isConflict())
    }
}
