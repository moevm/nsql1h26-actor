package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class UniversityServiceTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/test_university"
            }
        }
    }

    @Autowired
    lateinit var universityService: UniversityService

    @Autowired
    lateinit var universityRepository: UniversityRepository

    @BeforeEach
    fun setUp() {
        universityRepository.deleteAll()
    }

    @Test
    @DisplayName("create — только name")
    fun createMinimal() {
        val res = universityService.create(UniversityCreate(name = "МГУ"))
        assertThat(res.status).isEqualTo(com.NOSQL.NOSQL.model.generated.UniversityCreateResponse.Status.ok)
        assertThat(res.id).isNotNull()
        assertThat(res.errorCode).isNull()
        val doc = universityRepository.findById(res.id!!).get()
        assertThat(doc.name).isEqualTo("МГУ")
        assertThat(doc.shortName).isNull()
        assertThat(doc.oldNames).isNull()
    }

    @Test
    @DisplayName("create — name, shortName, oldNames")
    fun createFull() {
        val res = universityService.create(
            UniversityCreate(
                name = "Московский государственный университет",
                shortName = "МГУ",
                oldNames = listOf("Императорский Московский университет")
            )
        )
        assertThat(res.status).isEqualTo(com.NOSQL.NOSQL.model.generated.UniversityCreateResponse.Status.ok)
        val doc = universityRepository.findById(res.id!!).get()
        assertThat(doc.name).isEqualTo("Московский государственный университет")
        assertThat(doc.shortName).isEqualTo("МГУ")
        assertThat(doc.oldNames).containsExactly("Императорский Московский университет")
    }
}
