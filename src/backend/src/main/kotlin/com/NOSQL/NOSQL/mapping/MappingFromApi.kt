package com.NOSQL.NOSQL.mapping

import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.domain.ContactLinkItem as DomainContactLinkItem
import com.NOSQL.NOSQL.model.domain.EducationItem as DomainEducationItem
import com.NOSQL.NOSQL.model.domain.FilmPlayItem as DomainFilmPlayItem
import com.NOSQL.NOSQL.model.domain.Gender as DomainGender
import com.NOSQL.NOSQL.model.domain.TheatrePlayItem as DomainTheatrePlayItem
import com.NOSQL.NOSQL.model.domain.Title as DomainTitle
import com.NOSQL.NOSQL.model.domain.PhotoItem as DomainPhotoItem
import com.NOSQL.NOSQL.model.domain.VideoItem as DomainVideoItem
import com.NOSQL.NOSQL.model.generated.Actor
import com.NOSQL.NOSQL.model.generated.ActorCreate
import com.NOSQL.NOSQL.model.generated.ActorUpdate
import com.NOSQL.NOSQL.model.generated.EducationCreateItem
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
            education = actorCreate.education?.map(::educationCreateItemToDomain),
            films = actorCreate.films?.map(::filmPlayItemToDomain),
            theatrePlayItems = actorCreate.theatrePlayItems?.map(::theatrePlayItemToDomain),
            photos = emptyList(),
            videos = emptyList(),
            genres = actorCreate.genres,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant()
        )

    fun actorToDocument(a: Actor): ActorDocument =
        ActorDocument(
            id = a.id,
            firstName = a.firstName,
            lastName = a.lastName,
            middleName = a.middleName,
            birthDate = a.birthDate,
            height = a.height,
            weight = a.weight,
            gender = a.gender?.let { DomainGender.valueOf(it.name) },
            hairColor = a.hairColor,
            eyeColor = a.eyeColor,
            bio = a.bio,
            title = a.title?.let { DomainTitle.valueOf(it.name) },
            phone = a.phone,
            email = a.email,
            links = a.links?.map(::contactLinkItemToDomain),
            education = a.education?.map(::educationItemToDomain),
            films = a.films?.map(::filmPlayItemToDomain),
            theatrePlayItems = a.theatrePlayItems?.map(::theatrePlayItemToDomain),
            photos = a.photos?.map(::photoItemToDomain),
            mainPhotoId = a.mainPhotoId,
            videos = a.videos?.map(::videoItemToDomain),
            genres = a.genres,
            createdAt = a.createdAt?.toInstant(),
            updatedAt = a.updatedAt?.toInstant(),
        )

    fun mergeActorDocument(doc: ActorDocument, update: ActorUpdate): ActorDocument =
        doc.copy(
            firstName = update.firstName ?: doc.firstName,
            lastName = update.lastName ?: doc.lastName,
            middleName = update.middleName ?: doc.middleName,
            birthDate = update.birthDate ?: doc.birthDate,
            height = update.height ?: doc.height,
            weight = update.weight ?: doc.weight,
            gender = if (update.gender != null) DomainGender.valueOf(update.gender.name) else doc.gender,
            hairColor = update.hairColor ?: doc.hairColor,
            eyeColor = update.eyeColor ?: doc.eyeColor,
            bio = update.bio ?: doc.bio,
            title = if (update.title != null) DomainTitle.valueOf(update.title.name) else doc.title,
            phone = update.phone ?: doc.phone,
            email = update.email ?: doc.email,
            links = update.links?.map(::contactLinkItemToDomain) ?: doc.links,
            education = update.education?.map(::educationItemToDomain) ?: doc.education,
            films = update.films?.map(::filmPlayItemToDomain) ?: doc.films,
            theatrePlayItems = update.theatrePlayItems?.map(::theatrePlayItemToDomain) ?: doc.theatrePlayItems,
            genres = update.genres ?: doc.genres,
            mainPhotoId = update.mainPhotoId ?: doc.mainPhotoId,
        )

    private fun contactLinkItemToDomain(it: com.NOSQL.NOSQL.model.generated.ContactLinkItem): DomainContactLinkItem =
        DomainContactLinkItem(name = it.name, url = it.url?.toString())

    private fun educationCreateItemToDomain(it: EducationCreateItem): DomainEducationItem =
        DomainEducationItem(uniId = it.uniId, graduationYear = null, name = null)

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

    private fun photoItemToDomain(it: com.NOSQL.NOSQL.model.generated.PhotoItem): DomainPhotoItem =
        DomainPhotoItem(id = it.id, caption = it.caption)

    private fun videoItemToDomain(it: com.NOSQL.NOSQL.model.generated.VideoItem): DomainVideoItem =
        DomainVideoItem(id = it.id, caption = it.caption)
}
