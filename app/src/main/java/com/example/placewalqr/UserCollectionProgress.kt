package com.example.placewalqr

data class UserCollectionProgress(
    val collectionId: Int,
    val visitedPlaces: Int,
    val totalPlaces: Int,
    val visitedPlaceIds: List<Int> // ID dei luoghi visitati
)

