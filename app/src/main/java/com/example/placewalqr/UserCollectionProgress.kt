package com.example.placewalqr

// Data class for user progress in a collection
data class UserCollectionProgress(
    val collectionId: Int,           // ID of the collection
    val visitedPlaces: Int,          // Number of places visited
    val totalPlaces: Int,            // Total number of places in collection
    val visitedPlaceIds: List<Int>   // IDs of visited places
)