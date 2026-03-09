package com.NOSQL.NOSQL.model

import com.NOSQL.NOSQL.model.domain.EducationItem
import com.NOSQL.NOSQL.model.domain.FilmPlayItem
import com.NOSQL.NOSQL.model.domain.Gender
import com.NOSQL.NOSQL.model.domain.PhotoItem
import com.NOSQL.NOSQL.model.domain.TheatrePlayItem
import com.NOSQL.NOSQL.model.domain.Title
import com.NOSQL.NOSQL.model.domain.VideoItem
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

@Document(collection = "actors")
data class ActorDocument(
    @Id
    val id: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val birthDate: LocalDate? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val gender: Gender? = null,
    val hairColor: String? = null,
    val eyeColor: String? = null,
    val bio: String? = null,
    val title: Title? = null,
    val education: List<EducationItem>? = null,
    val films: List<FilmPlayItem>? = null,
    val theatrePlayItems: List<TheatrePlayItem>? = null,
    val photos: List<PhotoItem>? = null,
    val videos: List<VideoItem>? = null,
    val genres: List<String>? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
