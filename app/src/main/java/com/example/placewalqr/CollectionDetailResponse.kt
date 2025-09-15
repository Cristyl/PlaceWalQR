package com.example.placewalqr

// Response for collection details
data class CollectionDetailResponse(
    val collection: Collection,        // the collection info
    val places: List<CollectionPlace>  // the places inside the collection
)
