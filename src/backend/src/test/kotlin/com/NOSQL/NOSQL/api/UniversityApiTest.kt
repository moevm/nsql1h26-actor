package com.NOSQL.NOSQL.api

import com.NOSQL.NOSQL.repository.UniversityRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

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

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        universityRepository.deleteAll()
    }

    @Test
    @DisplayName("POST /v1/universities — 201, id и status ok")
    fun createUniversity() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"МГУ","shortName":"МГУ","oldNames":["Императорский"]}""")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.errorCode").isEmpty())
    }

    @Test
    @DisplayName("POST /v1/universities — только name")
    fun createUniversityMinimal() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"СПбГУ"}""")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    @DisplayName("GET /v1/universities/search — поиск по строке, возвращает id и названия")
    fun searchUniversities() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/universities")
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
}
