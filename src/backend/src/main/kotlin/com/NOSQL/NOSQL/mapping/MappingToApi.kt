package com.NOSQL.NOSQL.mapping

import com.NOSQL.NOSQL.model.ActorDocument
import com.NOSQL.NOSQL.model.domain.ContactLinkItem as DomainContactLinkItem
import com.NOSQL.NOSQL.model.domain.EducationItem as DomainEducationItem
import com.NOSQL.NOSQL.model.domain.FilmPlayItem as DomainFilmPlayItem
import com.NOSQL.NOSQL.model.domain.PhotoItem as DomainPhotoItem
import com.NOSQL.NOSQL.model.domain.TheatrePlayItem as DomainTheatrePlayItem
import com.NOSQL.NOSQL.model.domain.VideoItem as DomainVideoItem
import com.NOSQL.NOSQL.model.generated.Actor
import com.NOSQL.NOSQL.model.generated.ContactLinkItem as ApiContactLinkItem
import com.NOSQL.NOSQL.model.generated.EducationItem
import com.NOSQL.NOSQL.model.generated.FilmPlayItem
import com.NOSQL.NOSQL.model.generated.Gender
import com.NOSQL.NOSQL.model.generated.PhotoItem
import com.NOSQL.NOSQL.model.generated.TheatrePlayItem
import com.NOSQL.NOSQL.model.generated.Title
import com.NOSQL.NOSQL.model.generated.VideoItem
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

object MappingToApi {

    fun documentToActor(doc: ActorDocument): Actor =
        Actor(
            id = doc.id,
            firstName = doc.firstName,
            lastName = doc.lastName,
            middleName = doc.middleName,
            birthDate = doc.birthDate,
            height = doc.height,
            weight = doc.weight,
            gender = doc.gender?.let { Gender.valueOf(it.name) },
            hairColor = doc.hairColor,
            eyeColor = doc.eyeColor,
            bio = doc.bio,
            title = doc.title?.let { Title.valueOf(it.name) },
            phone = doc.phone,
            email = doc.email,
            links = doc.links?.map(::contactLinkItemToApi),
            education = doc.education?.map(::educationItemToApi),
            films = doc.films?.map(::filmPlayItemToApi),
            theatrePlayItems = doc.theatrePlayItems?.map(::theatrePlayItemToApi),
            photos = doc.photos?.map(::photoItemToApi),
            mainPhotoId = doc.mainPhotoId,
            videos = doc.videos?.map(::videoItemToApi),
            genres = doc.genres,
            createdAt = doc.createdAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) },
            updatedAt = doc.updatedAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) }
        )

    private fun contactLinkItemToApi(it: DomainContactLinkItem): ApiContactLinkItem =
        ApiContactLinkItem(name = it.name, url = it.url?.let { URI.create(it) })

    private fun educationItemToApi(it: DomainEducationItem): EducationItem =
        EducationItem(
            uniId = it.uniId,
            graduationYear = it.graduationYear,
            name = it.name
        )

    private fun filmPlayItemToApi(it: DomainFilmPlayItem): FilmPlayItem =
        FilmPlayItem(
            title = it.title,
            year = it.year,
            role = it.role,
            director = it.director
        )

    private fun photoItemToApi(it: DomainPhotoItem): PhotoItem =
        PhotoItem(
            id = it.id,
            caption = it.caption
        )

    private fun videoItemToApi(it: DomainVideoItem): VideoItem =
        VideoItem(
            id = it.id,
            caption = it.caption
        )

    private fun theatrePlayItemToApi(it: DomainTheatrePlayItem): TheatrePlayItem =
        TheatrePlayItem(
            name = it.name,
            years = it.years,
            plays = it.plays?.map(::filmPlayItemToApi)
        )
}
