// Data classes per le collections
package com.example.placewalqr

data class Collection(
    val id: Int,
    val name: String,
    val image: String?, // Base64 o URL
    val isUnknown: Boolean, // is_unknown dal DB
    val points: Int,
    val totalPlaces: Int = 0, // Calcolato
    val visitedPlaces: Int = 0 // Calcolato
) {
    // Nome da mostrare (??? se segreto e non completato)
    val displayName: String
        get() = if (isUnknown && visitedPlaces < totalPlaces) "???" else name

    // Immagine da mostrare (default se segreto e non completato)
    val displayImage: String?
        get() = if (isUnknown && visitedPlaces < totalPlaces) null else image

    // Se la collezione è completata
    val isCompleted: Boolean
        get() = visitedPlaces >= totalPlaces

    // Progress come stringa "5/7"
    val progressText: String
        get() = "$visitedPlaces/$totalPlaces"
}

