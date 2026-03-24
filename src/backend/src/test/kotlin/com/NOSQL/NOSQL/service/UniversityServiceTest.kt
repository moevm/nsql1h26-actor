package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.model.generated.UniversityUpdate
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
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
            val db = "test_university"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
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

    @Test
    @DisplayName("search — по полному названию (подстрока)")
    fun searchByName() {
        universityService.create(UniversityCreate(name = "Российский институт театрального искусства", shortName = "ГИТИС", oldNames = null))
        universityService.create(UniversityCreate(name = "Московский художественный театр", shortName = "МХАТ", oldNames = null))
        val list = universityService.search("театрального", 10)
        assertThat(list).hasSize(1)
        assertThat(list[0].name).isEqualTo("Российский институт театрального искусства")
        assertThat(list[0].id).isNotNull()
    }

    @Test
    @DisplayName("search — по краткому названию")
    fun searchByShortName() {
        universityService.create(UniversityCreate(name = "ГИТИС полное", shortName = "ГИТИС", oldNames = null))
        val list = universityService.search("ГИТИС", 10)
        assertThat(list).hasSize(1)
        assertThat(list[0].shortName).isEqualTo("ГИТИС")
    }

    @Test
    @DisplayName("search — по старым названиям")
    fun searchByOldNames() {
        universityService.create(
            UniversityCreate(
                name = "Щукинское училище",
                shortName = "Щука",
                oldNames = listOf("Театральное училище им. Щукина", "ВТУ им. Щукина")
            )
        )
        val list = universityService.search("Щукина", 10)
        assertThat(list).hasSize(1)
        assertThat(list[0].oldNames).contains("Театральное училище им. Щукина")
    }

    @Test
    @DisplayName("search — без учёта регистра")
    fun searchCaseInsensitive() {
        universityService.create(UniversityCreate(name = "Гнесинка", shortName = "Гнесинка", oldNames = null))
        assertThat(universityService.search("гнесинка", 10)).hasSize(1)
        assertThat(universityService.search("ГНЕСИНКА", 10)).hasSize(1)
    }

    @Test
    @DisplayName("search — пустой запрос возвращает пустой список")
    fun searchEmptyQuery() {
        universityService.create(UniversityCreate(name = "МГУ"))
        assertThat(universityService.search("", 10)).isEmpty()
        assertThat(universityService.search("   ", 10)).isEmpty()
    }

    @Test
    @DisplayName("search — limit ограничивает количество")
    fun searchLimit() {
        universityService.create(UniversityCreate(name = "Вуз А", shortName = "А", oldNames = null))
        universityService.create(UniversityCreate(name = "Вуз Б", shortName = "Б", oldNames = null))
        universityService.create(UniversityCreate(name = "Вуз В", shortName = "В", oldNames = null))
        val list = universityService.search("Вуз", 2)
        assertThat(list).hasSize(2)
    }

    @Test
    @DisplayName("update — частичное изменение name")
    fun updateName() {
        val id = universityService.create(UniversityCreate(name = "Старое", shortName = "С", oldNames = listOf("А"))).id!!
        val res = universityService.update(id, UniversityUpdate(name = "Новое"))
        assertThat(res.status).isEqualTo(com.NOSQL.NOSQL.model.generated.UniversityCreateResponse.Status.ok)
        val doc = universityRepository.findById(id).get()
        assertThat(doc.name).isEqualTo("Новое")
        assertThat(doc.shortName).isEqualTo("С")
        assertThat(doc.oldNames).containsExactly("А")
    }

    @Test
    @DisplayName("update — 404 если id не найден")
    fun updateNotFound() {
        assertThrows<ResponseStatusException> {
            universityService.update("000000000000000000000000", UniversityUpdate(name = "X"))
        }
    }

    @Test
    @DisplayName("update — 400 если нет полей")
    fun updateEmpty() {
        val id = universityService.create(UniversityCreate(name = "МГУ")).id!!
        assertThrows<ResponseStatusException> {
            universityService.update(id, UniversityUpdate())
        }
    }
}
