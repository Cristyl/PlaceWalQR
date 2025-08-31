package com.example.placewalqr

// Response per dettaglio collezione
data class CollectionDetailResponse(
    val collection: Collection,
    val places: List<CollectionPlace>
)