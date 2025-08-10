package com.example.placewalqr

data class VisitedPlaceResponse(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val id: Int,
    val visited: Boolean
)
