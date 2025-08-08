package com.example.placewalqr

data class VisitPlaceResponse(
    val status: String,
    val name: String,
    val information: String,
    val seen: Boolean
)