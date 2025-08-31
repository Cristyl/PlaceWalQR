package com.example.placewalqr

// Response dal server per collezioni utente
data class CollectionResponse(
    val collection: com.example.placewalqr.Collection,
    val userProgress: UserCollectionProgress
)