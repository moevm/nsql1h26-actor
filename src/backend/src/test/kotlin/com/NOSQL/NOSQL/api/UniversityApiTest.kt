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
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/test_university_api"
            }
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
}
