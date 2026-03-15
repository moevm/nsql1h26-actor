package com.NOSQL.NOSQL.mapping

import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.domain.ContactLinkItem as DomainContactLinkItem
import com.NOSQL.NOSQL.model.domain.EducationItem as DomainEducationItem
import com.NOSQL.NOSQL.model.domain.FilmPlayItem as DomainFilmPlayItem
import com.NOSQL.NOSQL.model.domain.Gender as DomainGender
import com.NOSQL.NOSQL.model.domain.TheatrePlayItem as DomainTheatrePlayItem
import com.NOSQL.NOSQL.model.domain.Title as DomainTitle
import com.NOSQL.NOSQL.model.generated.ActorCreate
import java.time.OffsetDateTime

object MappingFromApi {

    fun actorCreateToDocument(actorCreate: ActorCreate, createdAt: OffsetDateTime, updatedAt: OffsetDateTime): ActorDocument =
        ActorDocument(
            firstName = actorCreate.firstName,
            lastName = actorCreate.lastName,
            middleName = actorCreate.middleName,
            birthDate = actorCreate.birthDate,
            height = actorCreate.height,
            weight = actorCreate.weight,
            gender = actorCreate.gender?.let { DomainGender.valueOf(it.name) },
            hairColor = actorCreate.hairColor,
            eyeColor = actorCreate.eyeColor,
            bio = actorCreate.bio,
            title = actorCreate.title?.let { DomainTitle.valueOf(it.name) },
            phone = actorCreate.phone,
            email = actorCreate.email,
            links = actorCreate.links?.map(::contactLinkItemToDomain),
            education = actorCreate.education?.map(::educationItemToDomain),
            films = actorCreate.films?.map(::filmPlayItemToDomain),
            theatrePlayItems = actorCreate.theatrePlayItems?.map(::theatrePlayItemToDomain),
            photos = emptyList(),
            videos = emptyList(),
            genres = actorCreate.genres,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant()
        )

    private fun contactLinkItemToDomain(it: com.NOSQL.NOSQL.model.generated.ContactLinkItem): DomainContactLinkItem =
        DomainContactLinkItem(name = it.name, url = it.url?.toString())

    private fun educationItemToDomain(it: com.NOSQL.NOSQL.model.generated.EducationItem): DomainEducationItem =
        DomainEducationItem(
            uniId = it.uniId,
            graduationYear = it.graduationYear,
            name = it.name
        )

    private fun filmPlayItemToDomain(it: com.NOSQL.NOSQL.model.generated.FilmPlayItem): DomainFilmPlayItem =
        DomainFilmPlayItem(
            title = it.title,
            year = it.year,
            role = it.role,
            director = it.director
        )

    private fun theatrePlayItemToDomain(it: com.NOSQL.NOSQL.model.generated.TheatrePlayItem): DomainTheatrePlayItem =
        DomainTheatrePlayItem(
            name = it.name,
            years = it.years,
            plays = it.plays?.map(::filmPlayItemToDomain)
        )
}
