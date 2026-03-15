package com.NOSQL.NOSQL.mapping

import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.domain.ContactLinkItem as DomainContactLinkItem
import com.NOSQL.NOSQL.model.domain.EducationItem as DomainEducationItem
import com.NOSQL.NOSQL.model.domain.FilmPlayItem as DomainFilmPlayItem
import com.NOSQL.NOSQL.model.domain.Gender as DomainGender
import com.NOSQL.NOSQL.model.domain.PhotoItem as DomainPhotoItem
import com.NOSQL.NOSQL.model.domain.TheatrePlayItem as DomainTheatrePlayItem
import com.NOSQL.NOSQL.model.domain.Title as DomainTitle
import com.NOSQL.NOSQL.model.domain.VideoItem as DomainVideoItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MappingToApiTest {

    @Test
    @DisplayName("documentToActor — все поля, education без university")
    fun documentToActor_full() {
        val doc = ActorDocument(
            id = "507f1f77bcf86cd799439011",
            firstName = "Иван",
            lastName = "Петров",
            middleName = "Сергеевич",
            birthDate = LocalDate.of(1990, 5, 20),
            height = 180,
            weight = 75,
            gender = DomainGender.male,
            hairColor = "чёрный",
            eyeColor = "карий",
            bio = "Био",
            title = DomainTitle.national,
            phone = "+7 999 000-00-00",
            email = "test@example.com",
            links = listOf(
                DomainContactLinkItem(name = "ВК", url = "https://vk.com/id"),
                DomainContactLinkItem(name = "Личный сайт", url = "https://site.ru")
            ),
            education = listOf(DomainEducationItem(uniId = "uni1", graduationYear = 2012, name = "Актёр")),
            films = listOf(DomainFilmPlayItem(title = "Фильм", year = 2020, role = "Роль", director = "Реж")),
            theatrePlayItems = listOf(
                DomainTheatrePlayItem(
                    name = "Театр",
                    years = "2015–",
                    plays = listOf(DomainFilmPlayItem(title = "Пьеса", year = 2016, role = "Роль", director = null))
                )
            ),
            photos = listOf(DomainPhotoItem(id = "photo1", caption = "Фото")),
            videos = listOf(DomainVideoItem(id = "video1", caption = "Видео")),
            genres = listOf("драма"),
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
        val actor = MappingToApi.documentToActor(doc)
        assertThat(actor.id).isEqualTo("507f1f77bcf86cd799439011")
        assertThat(actor.firstName).isEqualTo("Иван")
        assertThat(actor.lastName).isEqualTo("Петров")
        assertThat(actor.middleName).isEqualTo("Сергеевич")
        assertThat(actor.birthDate).isEqualTo(LocalDate.of(1990, 5, 20))
        assertThat(actor.height).isEqualTo(180)
        assertThat(actor.weight).isEqualTo(75)
        assertThat(actor.gender?.name).isEqualTo("male")
        assertThat(actor.title?.name).isEqualTo("national")
        assertThat(actor.phone).isEqualTo("+7 999 000-00-00")
        assertThat(actor.email).isEqualTo("test@example.com")
        assertThat(actor.links).hasSize(2)
        assertThat(actor.links!![0].name).isEqualTo("ВК")
        assertThat(actor.links!![0].url).isEqualTo(URI("https://vk.com/id"))
        assertThat(actor.links!![1].name).isEqualTo("Личный сайт")
        assertThat(actor.links!![1].url).isEqualTo(URI("https://site.ru"))
        assertThat(actor.education).hasSize(1)
        assertThat(actor.education!![0].uniId).isEqualTo("uni1")
        assertThat(actor.education!![0].university).isNull()
        assertThat(actor.films).hasSize(1)
        assertThat(actor.theatrePlayItems).hasSize(1)
        assertThat(actor.photos).hasSize(1)
        assertThat(actor.photos!![0].id).isEqualTo("photo1")
        assertThat(actor.videos).hasSize(1)
        assertThat(actor.genres).containsExactly("драма")
        assertThat(actor.createdAt).isEqualTo(Instant.EPOCH.atOffset(ZoneOffset.UTC))
        assertThat(actor.updatedAt).isEqualTo(Instant.EPOCH.atOffset(ZoneOffset.UTC))
    }

    @Test
    @DisplayName("documentToActor — минимальный документ")
    fun documentToActor_minimal() {
        val doc = ActorDocument(
            id = "id1",
            firstName = "А",
            lastName = "Б"
        )
        val actor = MappingToApi.documentToActor(doc)
        assertThat(actor.id).isEqualTo("id1")
        assertThat(actor.firstName).isEqualTo("А")
        assertThat(actor.lastName).isEqualTo("Б")
        assertThat(actor.middleName).isNull()
        assertThat(actor.birthDate).isNull()
        assertThat(actor.gender).isNull()
        assertThat(actor.phone).isNull()
        assertThat(actor.email).isNull()
        assertThat(actor.links).isNull()
        assertThat(actor.education).isNull()
        assertThat(actor.photos).isNull()
        assertThat(actor.videos).isNull()
        assertThat(actor.createdAt).isNull()
    }
}
