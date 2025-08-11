package com.example.placewalqr

data class VisitPlaceRequest(
    val place_id: Int,
    val user_id: Int,
    val date_of_visit: String
)