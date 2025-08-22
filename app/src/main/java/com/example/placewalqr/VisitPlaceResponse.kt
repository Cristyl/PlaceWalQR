package com.example.placewalqr

data class VisitPlaceResponse(
    val status: String,
    val souvenir: Boolean,
    val name: String,
    val information: String,
    val image: String?,
    val seen: Boolean
)