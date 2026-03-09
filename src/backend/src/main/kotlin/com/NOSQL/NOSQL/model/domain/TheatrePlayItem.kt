package com.NOSQL.NOSQL.model.domain

data class TheatrePlayItem(
    val name: String? = null,
    val years: String? = null,
    val plays: List<FilmPlayItem>? = null
)
