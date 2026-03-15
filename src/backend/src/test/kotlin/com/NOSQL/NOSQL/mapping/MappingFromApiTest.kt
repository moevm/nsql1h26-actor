package com.NOSQL.NOSQL.mapping

import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ContactLinkItem
import com.NOSQL.NOSQL.model.generated.EducationItem
import com.NOSQL.NOSQL.model.generated.FilmPlayItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.TheatrePlayItem
import com.NOSQL.NOSQL.model.generated.Title
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime

class MappingFromApiTest {

    private val createdAt = OffsetDateTime.parse("2024-01-15T10:00:00Z")
    private val updatedAt = OffsetDateTime.parse("2024-01-15T10:00:00Z")

    @Test
    @DisplayName("actorCreateToDocument — все поля маппятся, photos/videos пустые")
    fun actorCreateToDocument_full() {
        val create = ActorCreate(
            firstName = "Иван",
            lastName = "Петров",
            middleName = "Сергеевич",
            birthDate = LocalDate.of(1990, 5, 20),
            height = 180,
            weight = 75,
            gender = Gender.male,
            hairColor = "чёрный",
            eyeColor = "карий",
            bio = "Био",
            title = Title.national,
            phone = "+7 999 123-45-67",
            email = "actor@test.ru",
            links = listOf(
                ContactLinkItem(name = "ВК", url = URI("https://vk.com/actor")),
                ContactLinkItem(name = "Рутуб", url = URI("https://rutube.ru/actor"))
            ),
            education = listOf(
                EducationItem(uniId = "507f1f77bcf86cd799439011", graduationYear = 2012, name = "Актёр")
            ),
            films = listOf(FilmPlayItem(title = "Фильм", year = 2020, role = "Роль", director = "Реж")),
            theatrePlayItems = listOf(
                TheatrePlayItem(name = "Театр", years = "2015–", plays = listOf(FilmPlayItem(title = "Пьеса", year = 2016, role = "Роль", director = null)))
            ),
            genres = listOf("драма", "комедия")
        )
        val doc = MappingFromApi.actorCreateToDocument(create, createdAt, updatedAt)
        assertThat(doc.firstName).isEqualTo("Иван")
        assertThat(doc.lastName).isEqualTo("Петров")
        assertThat(doc.middleName).isEqualTo("Сергеевич")
        assertThat(doc.birthDate).isEqualTo(LocalDate.of(1990, 5, 20))
        assertThat(doc.height).isEqualTo(180)
        assertThat(doc.weight).isEqualTo(75)
        assertThat(doc.gender?.name).isEqualTo("male")
        assertThat(doc.hairColor).isEqualTo("чёрный")
        assertThat(doc.eyeColor).isEqualTo("карий")
        assertThat(doc.bio).isEqualTo("Био")
        assertThat(doc.title?.name).isEqualTo("national")
        assertThat(doc.phone).isEqualTo("+7 999 123-45-67")
        assertThat(doc.email).isEqualTo("actor@test.ru")
        assertThat(doc.links).hasSize(2)
        assertThat(doc.links!![0].name).isEqualTo("ВК")
        assertThat(doc.links!![0].url).isEqualTo("https://vk.com/actor")
        assertThat(doc.links!![1].name).isEqualTo("Рутуб")
        assertThat(doc.links!![1].url).isEqualTo("https://rutube.ru/actor")
        assertThat(doc.education).hasSize(1)
        assertThat(doc.education!![0].uniId).isEqualTo("507f1f77bcf86cd799439011")
        assertThat(doc.education!![0].graduationYear).isEqualTo(2012)
        assertThat(doc.films).hasSize(1)
        assertThat(doc.films!![0].title).isEqualTo("Фильм")
        assertThat(doc.theatrePlayItems).hasSize(1)
        assertThat(doc.theatrePlayItems!![0].name).isEqualTo("Театр")
        assertThat(doc.theatrePlayItems!![0].plays).hasSize(1)
        assertThat(doc.photos).isEmpty()
        assertThat(doc.videos).isEmpty()
        assertThat(doc.genres).containsExactly("драма", "комедия")
        assertThat(doc.createdAt).isNotNull()
        assertThat(doc.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("actorCreateToDocument — минимальный create")
    fun actorCreateToDocument_minimal() {
        val create = ActorCreate(firstName = "А", lastName = "Б")
        val doc = MappingFromApi.actorCreateToDocument(create, createdAt, updatedAt)
        assertThat(doc.firstName).isEqualTo("А")
        assertThat(doc.lastName).isEqualTo("Б")
        assertThat(doc.middleName).isNull()
        assertThat(doc.birthDate).isNull()
        assertThat(doc.gender).isNull()
        assertThat(doc.title).isNull()
        assertThat(doc.phone).isNull()
        assertThat(doc.email).isNull()
        assertThat(doc.links).isNull()
        assertThat(doc.education).isNull()
        assertThat(doc.films).isNull()
        assertThat(doc.theatrePlayItems).isNull()
        assertThat(doc.genres).isNull()
        assertThat(doc.photos).isEmpty()
        assertThat(doc.videos).isEmpty()
    }
}
