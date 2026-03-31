package com.NOSQL.NOSQL.api

import com.NOSQL.NOSQL.model.AdminDocument
import com.NOSQL.NOSQL.model.catalog.CATALOG_FORMAT
import com.NOSQL.NOSQL.model.catalog.CATALOG_VERSION
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.AdminRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CatalogApiTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_catalog_api"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }
    }

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var adminRepository: AdminRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var universityRepository: UniversityRepository

    @Autowired
    lateinit var actorRepository: ActorRepository

    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
    private val testAdminEmail = "catalog-api@test.test"
    private val testAdminPassword = "testpass123"
    private lateinit var token: String

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mongoTemplate.db.getCollection("media.chunks").deleteMany(Document())
        mongoTemplate.db.getCollection("media.files").deleteMany(Document())
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        adminRepository.deleteAll()
        if (adminRepository.findByEmail(testAdminEmail) == null) {
            adminRepository.save(
                AdminDocument(
                    email = testAdminEmail,
                    passwordHash = passwordEncoder.encode(testAdminPassword)!!,
                    createdAt = Instant.now()
                )
            )
        }
        val securityConfigurer = SecurityMockMvcConfigurers.springSecurity()
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(securityConfigurer)
            .build()
        token = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$testAdminEmail","password":"$testAdminPassword"}""")
        ).andExpect(status().isOk).andReturn().response.contentAsString
            .let { objectMapper.readTree(it)["token"].asText() }
    }

    @Nested
    @DisplayName("GET /v1/catalog/export")
    inner class Export {
        @Test
        fun `401 без JWT`() {
            mockMvc.perform(MockMvcRequestBuilders.get("/v1/catalog/export"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `200 — JSON снимок`() {
            val res = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/catalog/export")
                    .header("Authorization", "Bearer $token")
            )
                .andExpect(status().isOk)
                .andReturn().response.contentAsString
            val root = objectMapper.readTree(res)
            assertThat(root["format"].asText()).isEqualTo(CATALOG_FORMAT)
            assertThat(root["version"].asInt()).isEqualTo(CATALOG_VERSION)
        }
    }

    @Nested
    @DisplayName("POST /v1/catalog/import")
    inner class Import {
        @Test
        fun `204 — импорт пустого снимка`() {
            val body = """{"format":"$CATALOG_FORMAT","version":$CATALOG_VERSION,"universities":[],"actors":[],"admins":[],"media":[]}"""
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/catalog/import")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isNoContent)
        }
    }
}
