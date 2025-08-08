package com.example.placewalqr

data class VisitPlaceRequest(
    val place_id: Int,
    val user_email: String,
    val date_of_visit: String
)