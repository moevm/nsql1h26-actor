package com.NOSQL.NOSQL.api

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorStatsFilters
import com.NOSQL.NOSQL.model.generated.ActorStatsRequest
import com.NOSQL.NOSQL.model.generated.ChartStatsGroupBy
import com.NOSQL.NOSQL.model.generated.ChartStatsXAxis
import com.NOSQL.NOSQL.model.generated.EducationCreateItem
import com.NOSQL.NOSQL.model.generated.FilmPlayItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.TheatrePlayItem
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import com.NOSQL.NOSQL.service.ActorService
import com.NOSQL.NOSQL.service.UniversityService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ActorChartStatsApiTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7")).withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_actor_chart_stats"
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
    lateinit var universityService: UniversityService

    @Autowired
    lateinit var actorService: ActorService

    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }

    private lateinit var uniId: String

    @BeforeEach
    fun setUp() {
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        val uniRes = universityService.create(UniversityCreate(name = "ГИТИС", shortName = "Г", oldNames = null))
        uniId = uniRes.id!!

        val birth = LocalDate.of(1990, 6, 15)
        val createNational = ActorCreate(
            firstName = "А",
            lastName = "Национальный",
            birthDate = birth,
            gender = Gender.male,
            height = 180,
            weight = 80,
            title = Title.national,
            hairColor = "чёрный",
            eyeColor = "карий",
            education = listOf(EducationCreateItem(uniId = uniId)),
            films = listOf(
                FilmPlayItem(title = "Ф1", year = 2010, role = null, director = null),
                FilmPlayItem(title = "Ф2", year = 2010, role = null, director = null),
            ),
            theatrePlayItems = listOf(
                TheatrePlayItem(
                    name = "Театр",
                    years = null,
                    plays = listOf(FilmPlayItem(title = "Пьеса", year = 2005, role = null, director = null)),
                ),
            ),
            genres = listOf("драма"),
        )
        actorService.create(createNational)

        val createHonored = ActorCreate(
            firstName = "Б",
            lastName = "Заслуженный",
            birthDate = birth,
            gender = Gender.female,
            height = 165,
            weight = 55,
            title = Title.honored,
            hairColor = "русый",
            eyeColor = "голубой",
            education = listOf(EducationCreateItem(uniId = uniId)),
            films = listOf(FilmPlayItem(title = "Ф3", year = 2015, role = null, director = null)),
            genres = listOf("комедия", "драма"),
        )
        actorService.create(createHonored)
    }

    @Nested
    @DisplayName("POST /v1/actors/stats")
    inner class PostStats {
        @Test
        fun `200 height and title — two series sorted by name`() {
            val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            val body = ActorStatsRequest(
                xAxis = ChartStatsXAxis.height,
                groupBy = ChartStatsGroupBy.title,
                filters = null,
            )
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series.length()").value(2))
                .andExpect(jsonPath("$.series[0].name").value("Заслуженный артист"))
                .andExpect(jsonPath("$.series[0].data[0].x").value(165))
                .andExpect(jsonPath("$.series[0].data[0].value").value(1))
                .andExpect(jsonPath("$.series[1].name").value("Народный артист"))
                .andExpect(jsonPath("$.series[1].data[0].x").value(180))
                .andExpect(jsonPath("$.series[1].data[0].value").value(1))
        }

        @Test
        fun `200 filmYear counts two films same year`() {
            val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            val body = ActorStatsRequest(
                xAxis = ChartStatsXAxis.filmYear,
                groupBy = ChartStatsGroupBy.gender,
                filters = null,
            )
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series.length()").value(2))
                .andExpect(jsonPath("$.series[0].name").value("Женский"))
                .andExpect(jsonPath("$.series[1].name").value("Мужской"))
            val res = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            ).andReturn()
            val root = objectMapper.readTree(res.response.contentAsString)
            val maleSeries = root["series"].firstOrNull { it["name"].asText() == "Мужской" }
            assertThat(maleSeries).isNotNull
            val point2010 = maleSeries!!["data"].firstOrNull { it["x"].asInt() == 2010 }
            assertThat(point2010).isNotNull
            assertThat(point2010!!["value"].asInt()).isEqualTo(2)
        }

        @Test
        fun `200 filter gender male only national title series`() {
            val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            val body = ActorStatsRequest(
                xAxis = ChartStatsXAxis.weight,
                groupBy = ChartStatsGroupBy.title,
                filters = ActorStatsFilters(gender = Gender.male),
            )
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series.length()").value(1))
                .andExpect(jsonPath("$.series[0].name").value("Народный артист"))
                .andExpect(jsonPath("$.series[0].data[0].x").value(80))
        }

        @Test
        fun `200 groupBy university uses catalog name`() {
            val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            val body = ActorStatsRequest(
                xAxis = ChartStatsXAxis.birthYear,
                groupBy = ChartStatsGroupBy.university,
                filters = null,
            )
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series.length()").value(1))
                .andExpect(jsonPath("$.series[0].name").value("ГИТИС"))
                .andExpect(jsonPath("$.series[0].data[0].x").value(1990))
                .andExpect(jsonPath("$.series[0].data[0].value").value(2))
        }

        @Test
        fun `200 playYear`() {
            val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            val body = ActorStatsRequest(
                xAxis = ChartStatsXAxis.playYear,
                groupBy = ChartStatsGroupBy.genre,
                filters = ActorStatsFilters(gender = Gender.male),
            )
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].name").value("драма"))
                .andExpect(jsonPath("$.series[0].data[0].x").value(2005))
                .andExpect(jsonPath("$.series[0].data[0].value").value(1))
        }

        @Test
        fun `400 invalid xAxis in JSON`() {
            val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
            mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/actors/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"xAxis":"notAnAxis","groupBy":"gender"}"""),
            )
                .andExpect(status().isBadRequest())
        }
    }
}
