package com.example.placewalqr

// Server response for a user collection
data class CollectionResponse(
    val collection: com.example.placewalqr.Collection, // Collection info
    val userProgress: UserCollectionProgress          // User progress for this collection
)
