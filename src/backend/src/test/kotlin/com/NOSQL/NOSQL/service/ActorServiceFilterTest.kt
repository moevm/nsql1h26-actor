package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.EducationItem
import com.NOSQL.NOSQL.model.generated.FilmPlayItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.TheatrePlayItem
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.repository.ActorRepository
import com.NOSQL.NOSQL.repository.UniversityRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import java.time.LocalDate

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ActorServiceFilterTest {

    companion object {
        @Container
        @JvmStatic
        val mongo = GenericContainer(DockerImageName.parse("mongo:7"))
            .withExposedPorts(27017)

        @DynamicPropertySource
        @JvmStatic
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            val db = "test_actor_filters"
            registry.add("spring.data.mongodb.uri") {
                "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/$db"
            }
            registry.add("spring.data.mongodb.database") { db }
        }
    }

    @Autowired
    lateinit var actorService: ActorService

    @Autowired
    lateinit var universityService: UniversityService

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var universityRepository: UniversityRepository

    private lateinit var uniA: String
    private lateinit var uniB: String
    private lateinit var idMale40: String
    private lateinit var idFemale35: String
    private lateinit var idMale25: String
    private lateinit var idOleg: String
    private lateinit var idWithMiddleName: String

    @BeforeEach
    fun setUp() {
        actorRepository.deleteAll()
        universityRepository.deleteAll()
        val now = LocalDate.now()
        // Университеты
        val resA = universityService.create(UniversityCreate(name = "Вуз А", shortName = "А", oldNames = null))
        val resB = universityService.create(UniversityCreate(name = "Вуз Б", shortName = "Б", oldNames = null))
        assertThat(resA.status).isEqualTo(com.NOSQL.NOSQL.model.generated.UniversityCreateResponse.Status.ok)
        assertThat(resB.status).isEqualTo(com.NOSQL.NOSQL.model.generated.UniversityCreateResponse.Status.ok)
        uniA = resA.id!!
        uniB = resB.id!!

        // Актёр 1: мужчина, 40 лет, вес 70, чёрные волосы, карие глаза, народный, театр Современник, жанр драма, фильмы 2010 и 2015, вуз А
        val create1 = actorService.create(
            ActorCreate(
                firstName = "Иван",
                lastName = "Петров",
                middleName = null,
                birthDate = now.minusYears(40),
                height = 180,
                weight = 70,
                gender = Gender.male,
                hairColor = "чёрный",
                eyeColor = "карий",
                bio = null,
                title = Title.national,
                education = listOf(EducationItem(uniId = uniA, graduationYear = 2005, name = "Актёр")),
                films = listOf(
                    FilmPlayItem(title = "Фильм 1", year = 2010, role = "Роль 1", director = "Реж 1"),
                    FilmPlayItem(title = "Фильм 2", year = 2015, role = "Роль 2", director = "Реж 2")
                ),
                theatrePlayItems = listOf(
                    TheatrePlayItem(name = "Современник", years = "2005–", plays = listOf(FilmPlayItem(title = "Пьеса 1", year = 2006, role = "Роль", director = null)))
                ),
                genres = listOf("драма")
            )
        )
        idMale40 = create1.id!!

        // Актёр 2: женщина, 35 лет, вес 60, русые, голубые, заслуженная, театр Ленком, жанр комедия, фильм 2012, вуз Б
        val create2 = actorService.create(
            ActorCreate(
                firstName = "Мария",
                lastName = "Сидорова",
                middleName = null,
                birthDate = now.minusYears(35),
                height = 165,
                weight = 60,
                gender = Gender.female,
                hairColor = "русый",
                eyeColor = "голубой",
                bio = null,
                title = Title.honored,
                education = listOf(EducationItem(uniId = uniB, graduationYear = 2010, name = "Актёр")),
                films = listOf(FilmPlayItem(title = "Комедия", year = 2012, role = "Главная", director = "Реж")),
                theatrePlayItems = listOf(
                    TheatrePlayItem(name = "Ленком", years = "2010–", plays = listOf(FilmPlayItem(title = "Пьеса", year = 2011, role = "Роль", director = null)))
                ),
                genres = listOf("комедия")
            )
        )
        idFemale35 = create2.id!!

        // Актёр 3: мужчина, 25 лет, вес 80, чёрные волосы, без звания, театр Современник, жанры драма и комедия, фильм 2020
        val create3 = actorService.create(
            ActorCreate(
                firstName = "Алексей",
                lastName = "Козлов",
                middleName = null,
                birthDate = now.minusYears(25),
                height = 178,
                weight = 80,
                gender = Gender.male,
                hairColor = "чёрный",
                eyeColor = null,
                bio = null,
                title = Title.none,
                education = null,
                films = listOf(FilmPlayItem(title = "Новый фильм", year = 2020, role = "Эпизод", director = null)),
                theatrePlayItems = listOf(
                    TheatrePlayItem(name = "Современник", years = "2020–", plays = listOf(FilmPlayItem(title = "Пьеса 2020", year = 2020, role = "Роль", director = null)))
                ),
                genres = listOf("драма", "комедия")
            )
        )
        idMale25 = create3.id!!

        val create4 = actorService.create(
            ActorCreate(
                firstName = "Олег",
                lastName = "Смирнов",
                middleName = null,
                birthDate = now.minusYears(30),
                gender = Gender.male,
                education = null,
                films = null,
                theatrePlayItems = null,
                genres = null
            )
        )
        idOleg = create4.id!!

        // Актёр с отчеством — для тестов полного ФИО
        val create5 = actorService.create(
            ActorCreate(
                firstName = "Николай",
                lastName = "Иванов",
                middleName = "Михайлович",
                birthDate = now.minusYears(28),
                gender = Gender.male,
                education = null,
                films = null,
                theatrePlayItems = null,
                genres = null
            )
        )
        idWithMiddleName = create5.id!!
    }

    private fun findAll(
        gender: Gender? = null,
        ageFrom: Int? = null,
        ageTo: Int? = null,
        weightMin: Int? = null,
        weightMax: Int? = null,
        heightMin: Int? = null,
        heightMax: Int? = null,
        activityYearFrom: Int? = null,
        activityYearTo: Int? = null,
        universityId: String? = null,
        theatre: String? = null,
        title: Title? = null,
        hairColor: String? = null,
        eyeColor: String? = null,
        genres: List<String>? = null,
        name: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ) = actorService.findAll(
        gender = gender,
        ageFrom = ageFrom,
        ageTo = ageTo,
        weightMin = weightMin,
        weightMax = weightMax,
        heightMin = heightMin,
        heightMax = heightMax,
        activityYearFrom = activityYearFrom,
        activityYearTo = activityYearTo,
        universityId = universityId,
        theatre = theatre,
        title = title,
        hairColor = hairColor,
        eyeColor = eyeColor,
        genres = genres,
        name = name,
        limit = limit,
        offset = offset
    )

    @Nested
    @DisplayName("Без фильтров")
    inner class NoFilters {
        @Test
        fun `findAll без фильтров возвращает всех пяти`() {
            val list = findAll()
            assertThat(list).hasSize(5)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idFemale35, idMale25, idOleg, idWithMiddleName)
        }

        @Test
        fun `limit и offset работают`() {
            assertThat(findAll(limit = 2, offset = 0)).hasSize(2)
            assertThat(findAll(limit = 2, offset = 1)).hasSize(2)
            assertThat(findAll(limit = 2, offset = 2)).hasSize(2)
            assertThat(findAll(limit = 1, offset = 4)).hasSize(1)
            assertThat(findAll(limit = 1, offset = 0)).hasSize(1)
        }
    }

    @Nested
    @DisplayName("Фильтр по полу")
    inner class GenderFilter {
        @Test
        fun `gender male — четыре актёра`() {
            val list = findAll(gender = Gender.male)
            assertThat(list).hasSize(4)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25, idOleg, idWithMiddleName)
        }

        @Test
        fun `gender female — один актёр`() {
            val list = findAll(gender = Gender.female)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }
    }

    @Nested
    @DisplayName("Фильтр по возрасту")
    inner class AgeFilter {
        @Test
        fun `ageFrom 30 ageTo 45 — три актёра 40, 35 и 30 лет`() {
            val list = findAll(ageFrom = 30, ageTo = 45)
            assertThat(list).hasSize(3)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idFemale35, idOleg)
        }

        @Test
        fun `ageTo 30 — три актёра 25, 28 и 30 лет`() {
            val list = findAll(ageTo = 30)
            assertThat(list).hasSize(3)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale25, idOleg, idWithMiddleName)
        }

        @Test
        fun `ageFrom 36 — один актёр 40 лет`() {
            val list = findAll(ageFrom = 36)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }
    }

    @Nested
    @DisplayName("Фильтр по росту")
    inner class HeightFilter {
        @Test
        fun `heightMin 170 heightMax 185 — два актёра с ростом 180 и 178`() {
            val list = findAll(heightMin = 170, heightMax = 185)
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `heightMax 170 — один актёр с ростом 165`() {
            val list = findAll(heightMax = 170)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `heightMin 179 — один с ростом 180`() {
            val list = findAll(heightMin = 179)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }
    }

    @Nested
    @DisplayName("Фильтр по весу")
    inner class WeightFilter {
        @Test
        fun `weightMin 65 weightMax 75 — один с весом 70`() {
            val list = findAll(weightMin = 65, weightMax = 75)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `weightMax 65 — один актёр с весом 60`() {
            val list = findAll(weightMax = 65)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `weightMin 75 — один с весом 80`() {
            val list = findAll(weightMin = 75)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale25)
        }
    }

    @Nested
    @DisplayName("Фильтр по внешности")
    inner class AppearanceFilter {
        @Test
        fun `hairColor чёрный — два актёра`() {
            val list = findAll(hairColor = "чёрный")
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `eyeColor карий — один`() {
            val list = findAll(eyeColor = "карий")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `eyeColor голубой — один`() {
            val list = findAll(eyeColor = "голубой")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }
    }

    @Nested
    @DisplayName("Фильтр по званию")
    inner class TitleFilter {
        @Test
        fun `title national — один`() {
            val list = findAll(title = Title.national)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `title honored — один`() {
            val list = findAll(title = Title.honored)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `title none — один`() {
            val list = findAll(title = Title.none)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale25)
        }
    }

    @Nested
    @DisplayName("Фильтр по театру")
    inner class TheatreFilter {
        @Test
        fun `theatre Современник — два актёра`() {
            val list = findAll(theatre = "Современник")
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `theatre Ленком — один`() {
            val list = findAll(theatre = "Ленком")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `theatre регистронезависимый`() {
            val list = findAll(theatre = "современник")
            assertThat(list).hasSize(2)
        }
    }

    @Nested
    @DisplayName("Фильтр по вузу")
    inner class UniversityFilter {
        @Test
        fun `universityId uniA — один`() {
            val list = findAll(universityId = uniA)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `universityId uniB — один`() {
            val list = findAll(universityId = uniB)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `universityId несуществующий — ноль`() {
            val list = findAll(universityId = "000000000000000000000000")
            assertThat(list).isEmpty()
        }
    }

    @Nested
    @DisplayName("Фильтр по жанрам")
    inner class GenresFilter {
        @Test
        fun `genres драма — два (у обоих есть драма)`() {
            val list = findAll(genres = listOf("драма"))
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `genres комедия — два`() {
            val list = findAll(genres = listOf("комедия"))
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idFemale35, idMale25)
        }

        @Test
        fun `genres драма и комедия — один (только у Козлова оба)`() {
            val list = findAll(genres = listOf("драма", "комедия"))
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale25)
        }
    }

    @Nested
    @DisplayName("Фильтр по годам активности")
    inner class ActivityYearFilter {
        @Test
        fun `activityYearFrom 2018 activityYearTo 2022 — один с фильмом 2020`() {
            val list = findAll(activityYearFrom = 2018, activityYearTo = 2022)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale25)
        }

        @Test
        fun `activityYear 2010–2015 — два актёра (Петров 2010 и 2015, Сидорова 2012 и пьеса 2011)`() {
            val list = findAll(activityYearFrom = 2010, activityYearTo = 2015)
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idFemale35)
        }

        @Test
        fun `activityYear 2012 — один с фильмом 2012`() {
            val list = findAll(activityYearFrom = 2012, activityYearTo = 2012)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }
    }

    @Nested
    @DisplayName("Фильтр по ФИО (подстрока и полное имя)")
    inner class NameFilter {
        @Test
        fun `name Олег находит по имени`() {
            val list = findAll(name = "Олег")
            assertThat(list).hasSize(1)
            assertThat(list[0].firstName).isEqualTo("Олег")
        }

        @Test
        fun `name Смирн находит по части фамилии`() {
            val list = findAll(name = "Смирн")
            assertThat(list).hasSize(1)
            assertThat(list[0].lastName).isEqualTo("Смирнов")
        }

        @Test
        fun `name Иван Петров находит по имени и фамилии`() {
            val list = findAll(name = "Иван Петров")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name Петров Иван находит по фамилии и имени (ФИО)`() {
            val list = findAll(name = "Петров Иван")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name Мария Сидорова находит актрису`() {
            val list = findAll(name = "Мария Сидорова")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `name Олег Смирнов находит по полному имени без отчества`() {
            val list = findAll(name = "Олег Смирнов")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idOleg)
        }

        @Test
        fun `name Смирнов Олег находит по ФИО порядку`() {
            val list = findAll(name = "Смирнов Олег")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idOleg)
        }

        @Test
        fun `name Иванов Николай Михайлович находит по полному ФИО`() {
            val list = findAll(name = "Иванов Николай Михайлович")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idWithMiddleName)
        }

        @Test
        fun `name Николай Иванов Михайлович находит по имени фамилии отчеству`() {
            val list = findAll(name = "Николай Иванов Михайлович")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idWithMiddleName)
        }

        @Test
        fun `name Иванов Михайлович находит по фамилии и отчеству`() {
            val list = findAll(name = "Иванов Михайлович")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idWithMiddleName)
        }

        @Test
        fun `name иван петров регистронезависимый поиск`() {
            val list = findAll(name = "иван петров")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name пустая строка не фильтрует`() {
            val list = findAll(name = "")
            assertThat(list).hasSize(5)
        }

        @Test
        fun `name пробелы обрезаются`() {
            val list = findAll(name = "  Иван Петров  ")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name несуществующее ФИО — ноль результатов`() {
            val list = findAll(name = "Несуществующий Актёр")
            assertThat(list).isEmpty()
        }

        @Test
        fun `name перепутанное ФИО Петров Мария — ноль результатов`() {
            val list = findAll(name = "Петров Мария")
            assertThat(list).isEmpty()
        }
    }

    @Nested
    @DisplayName("Комбинации фильтров")
    inner class CombinedFilters {
        @Test
        fun `male + театр Современник — два актёра`() {
            val list = findAll(gender = Gender.male, theatre = "Современник")
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `male + вес от 65 до 75 — один`() {
            val list = findAll(gender = Gender.male, weightMin = 65, weightMax = 75)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `возраст 30–45 + жанр драма — один (Петров 40 лет)`() {
            val list = findAll(ageFrom = 30, ageTo = 45, genres = listOf("драма"))
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }
    }
}
