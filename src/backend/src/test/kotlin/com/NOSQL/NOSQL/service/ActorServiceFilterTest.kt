package com.NOSQL.NOSQL.service

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.EducationCreateItem
import com.NOSQL.NOSQL.model.generated.FilmPlayItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.TheatrePlayItem
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.model.generated.UniversityCreate
import com.NOSQL.NOSQL.model.generated.UniversityCreateResponse
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
        // Universities
        val resA = universityService.create(UniversityCreate(name = "Вуз А", shortName = "А", oldNames = null))
        val resB = universityService.create(UniversityCreate(name = "Вуз Б", shortName = "Б", oldNames = null))
        assertThat(resA.status).isEqualTo(UniversityCreateResponse.Status.ok)
        assertThat(resB.status).isEqualTo(UniversityCreateResponse.Status.ok)
        uniA = resA.id!!
        uniB = resB.id!!

        // Actor 1: male, 40, weight 70, black hair, brown eyes, national title, Sovremennik theatre, drama, films 2010 & 2015, uni A
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
                education = listOf(EducationCreateItem(uniId = uniA)),
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

        // Actor 2: female, 35, weight 60, fair hair, blue eyes, honored title, Lenkom theatre, comedy, film 2012, uni B
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
                education = listOf(EducationCreateItem(uniId = uniB)),
                films = listOf(FilmPlayItem(title = "Комедия", year = 2012, role = "Главная", director = "Реж")),
                theatrePlayItems = listOf(
                    TheatrePlayItem(name = "Ленком", years = "2010–", plays = listOf(FilmPlayItem(title = "Пьеса", year = 2011, role = "Роль", director = null)))
                ),
                genres = listOf("комедия")
            )
        )
        idFemale35 = create2.id!!

        // Actor 3: male, 25, weight 80, black hair, no title, Sovremennik, drama & comedy, film 2020
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

        // Actor with patronymic — full-name search tests
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

    private fun findAllResult(
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
        offset: Int = 0,
        includeItems: Boolean = true,
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
        offset = offset,
        includeItems = includeItems,
    )

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
        offset: Int = 0,
    ) = findAllResult(
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
        offset = offset,
    ).actors

    @Nested
    @DisplayName("No filters")
    inner class NoFilters {
        @Test
        fun `findAll with no filters returns all five`() {
            val list = findAll()
            assertThat(list).hasSize(5)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idFemale35, idMale25, idOleg, idWithMiddleName)
        }

        @Test
        fun `limit and offset work`() {
            assertThat(findAll(limit = 2, offset = 0)).hasSize(2)
            assertThat(findAll(limit = 2, offset = 1)).hasSize(2)
            assertThat(findAll(limit = 2, offset = 2)).hasSize(2)
            assertThat(findAll(limit = 1, offset = 4)).hasSize(1)
            assertThat(findAll(limit = 1, offset = 0)).hasSize(1)
        }

        @Test
        fun `total ignores limit and offset`() {
            val r = findAllResult(limit = 2, offset = 0)
            assertThat(r.total).isEqualTo(5)
            assertThat(r.actors).hasSize(2)
        }

        @Test
        fun `includeItems false leaves actors empty but keeps total`() {
            val r = findAllResult(limit = 10, offset = 0, includeItems = false)
            assertThat(r.total).isEqualTo(5)
            assertThat(r.actors).isEmpty()
        }
    }

    @Nested
    @DisplayName("Gender filter")
    inner class GenderFilter {
        @Test
        fun `gender male returns four actors`() {
            val list = findAll(gender = Gender.male)
            assertThat(list).hasSize(4)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25, idOleg, idWithMiddleName)
        }

        @Test
        fun `gender female returns one actor`() {
            val list = findAll(gender = Gender.female)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }
    }

    @Nested
    @DisplayName("Age filter")
    inner class AgeFilter {
        @Test
        fun `ageFrom 30 ageTo 45 returns three actors aged 40 35 and 30`() {
            val list = findAll(ageFrom = 30, ageTo = 45)
            assertThat(list).hasSize(3)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idFemale35, idOleg)
        }

        @Test
        fun `ageTo 30 returns three actors aged 25 28 and 30`() {
            val list = findAll(ageTo = 30)
            assertThat(list).hasSize(3)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale25, idOleg, idWithMiddleName)
        }

        @Test
        fun `ageFrom 36 returns one actor aged 40`() {
            val list = findAll(ageFrom = 36)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `ageTo includes peers whose birthday before anniversary this year`() {
            val now = LocalDate.now()
            val res = actorService.create(
                ActorCreate(
                    firstName = "Граница",
                    lastName = "Возраста",
                    middleName = null,
                    birthDate = now.minusYears(48).minusDays(1),
                    gender = Gender.male,
                    education = null,
                    films = null,
                    theatrePlayItems = null,
                    genres = null
                )
            )
            val idBoundary = res.id!!
            assertThat(findAll(ageTo = 48).map { it.id }).contains(idBoundary)
            assertThat(findAll(ageTo = 47).map { it.id }).doesNotContain(idBoundary)
        }
    }

    @Nested
    @DisplayName("Height filter")
    inner class HeightFilter {
        @Test
        fun `heightMin 170 heightMax 185 returns two actors at 180 and 178`() {
            val list = findAll(heightMin = 170, heightMax = 185)
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `heightMax 170 returns one actor at 165`() {
            val list = findAll(heightMax = 170)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `heightMin 179 returns one actor at 180`() {
            val list = findAll(heightMin = 179)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }
    }

    @Nested
    @DisplayName("Weight filter")
    inner class WeightFilter {
        @Test
        fun `weightMin 65 weightMax 75 returns one actor at 70`() {
            val list = findAll(weightMin = 65, weightMax = 75)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `weightMax 65 returns one actor at 60`() {
            val list = findAll(weightMax = 65)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `weightMin 75 returns one actor at 80`() {
            val list = findAll(weightMin = 75)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale25)
        }
    }

    @Nested
    @DisplayName("Appearance filter")
    inner class AppearanceFilter {
        @Test
        fun `hairColor black returns two actors`() {
            val list = findAll(hairColor = "чёрный")
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `eyeColor brown returns one`() {
            val list = findAll(eyeColor = "карий")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `eyeColor blue returns one`() {
            val list = findAll(eyeColor = "голубой")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }
    }

    @Nested
    @DisplayName("Title filter")
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
    @DisplayName("Theatre filter")
    inner class TheatreFilter {
        @Test
        fun `theatre Sovremennik returns two actors`() {
            val list = findAll(theatre = "Современник")
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `theatre Lenkom returns one`() {
            val list = findAll(theatre = "Ленком")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `theatre filter is case insensitive`() {
            val list = findAll(theatre = "современник")
            assertThat(list).hasSize(2)
        }
    }

    @Nested
    @DisplayName("University filter")
    inner class UniversityFilter {
        @Test
        fun `universityId uniA returns one`() {
            val list = findAll(universityId = uniA)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `universityId uniB returns one`() {
            val list = findAll(universityId = uniB)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `universityId nonexistent returns zero`() {
            val list = findAll(universityId = "000000000000000000000000")
            assertThat(list).isEmpty()
        }
    }

    @Nested
    @DisplayName("Genres filter")
    inner class GenresFilter {
        @Test
        fun `genres drama returns two both have drama`() {
            val list = findAll(genres = listOf("драма"))
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `genres comedy returns two`() {
            val list = findAll(genres = listOf("комедия"))
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idFemale35, idMale25)
        }

        @Test
        fun `genres drama and comedy returns one only Kozlov has both`() {
            val list = findAll(genres = listOf("драма", "комедия"))
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale25)
        }
    }

    @Nested
    @DisplayName("Activity year filter")
    inner class ActivityYearFilter {
        @Test
        fun `activityYearFrom 2018 activityYearTo 2022 returns one with film 2020`() {
            val list = findAll(activityYearFrom = 2018, activityYearTo = 2022)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale25)
        }

        @Test
        fun `activityYear 2010-2015 returns two Petrov films 2010 and 2015 Sidorova 2012 and play 2011`() {
            val list = findAll(activityYearFrom = 2010, activityYearTo = 2015)
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idFemale35)
        }

        @Test
        fun `activityYear 2012 returns one with film 2012`() {
            val list = findAll(activityYearFrom = 2012, activityYearTo = 2012)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }
    }

    @Nested
    @DisplayName("Name filter (substring and full name)")
    inner class NameFilter {
        @Test
        fun `name Oleg matches by first name`() {
            val list = findAll(name = "Олег")
            assertThat(list).hasSize(1)
            assertThat(list[0].firstName).isEqualTo("Олег")
        }

        @Test
        fun `name Smirn matches by last name substring`() {
            val list = findAll(name = "Смирн")
            assertThat(list).hasSize(1)
            assertThat(list[0].lastName).isEqualTo("Смирнов")
        }

        @Test
        fun `name Ivan Petrov matches first and last`() {
            val list = findAll(name = "Иван Петров")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name Petrov Ivan matches last then first`() {
            val list = findAll(name = "Петров Иван")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name Maria Sidorova finds actress`() {
            val list = findAll(name = "Мария Сидорова")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idFemale35)
        }

        @Test
        fun `name Oleg Smirnov full name without patronymic`() {
            val list = findAll(name = "Олег Смирнов")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idOleg)
        }

        @Test
        fun `name Smirnov Oleg last first order`() {
            val list = findAll(name = "Смирнов Олег")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idOleg)
        }

        @Test
        fun `name Ivanov Nikolay Mikhaylovich full FIO`() {
            val list = findAll(name = "Иванов Николай Михайлович")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idWithMiddleName)
        }

        @Test
        fun `name Nikolay Ivanov Mikhaylovich first last patronymic`() {
            val list = findAll(name = "Николай Иванов Михайлович")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idWithMiddleName)
        }

        @Test
        fun `name Ivanov Mikhaylovich last and patronymic`() {
            val list = findAll(name = "Иванов Михайлович")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idWithMiddleName)
        }

        @Test
        fun `name ivan petrov case insensitive`() {
            val list = findAll(name = "иван петров")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name empty string does not filter`() {
            val list = findAll(name = "")
            assertThat(list).hasSize(5)
        }

        @Test
        fun `name trims whitespace`() {
            val list = findAll(name = "  Иван Петров  ")
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `name unknown full name returns zero`() {
            val list = findAll(name = "Несуществующий Актёр")
            assertThat(list).isEmpty()
        }

        @Test
        fun `name swapped Petrov Maria returns zero`() {
            val list = findAll(name = "Петров Мария")
            assertThat(list).isEmpty()
        }
    }

    @Nested
    @DisplayName("Combined filters")
    inner class CombinedFilters {
        @Test
        fun `male plus Sovremennik theatre returns two`() {
            val list = findAll(gender = Gender.male, theatre = "Современник")
            assertThat(list).hasSize(2)
            assertThat(list.map { it.id }).containsExactlyInAnyOrder(idMale40, idMale25)
        }

        @Test
        fun `male plus weight 65 to 75 returns one`() {
            val list = findAll(gender = Gender.male, weightMin = 65, weightMax = 75)
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }

        @Test
        fun `age 30-45 plus drama genre returns one Petrov 40`() {
            val list = findAll(ageFrom = 30, ageTo = 45, genres = listOf("драма"))
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(idMale40)
        }
    }
}
