package com.example.placewalqr

data class CollectionPlace(
    val id: Int,
    val name: String,
    val image: String?,
    val isVisited: Boolean,
    val isSecret: Boolean = false // Per luoghi segreti in collezioni segrete
) {
    // Nome da mostrare per luoghi segreti non visitati
    val displayName: String
        get() = if (isSecret && !isVisited) "???" else name

    // Immagine da mostrare per luoghi segreti non visitati  
    val displayImage: String?
        get() = if (isSecret && !isVisited) null else image
}



