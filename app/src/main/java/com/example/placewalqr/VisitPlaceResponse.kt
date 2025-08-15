package com.example.placewalqr

import java.sql.Blob

data class VisitPlaceResponse(
    val status: String,
    val name: String,
    val information: String,
    val image: Blob,
    val seen: Boolean
)