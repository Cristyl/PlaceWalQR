package com.example.placewalqr

data class LastPlaceResponse(
    val status: String,
    val place_id: Int,
    val name: String,
    val information: String,
    val date: String,
    val img_path: String
)
