package com.example.placewalqr

// Data class for places inside a collection
data class CollectionPlace(
    val id: Int,            // Place ID
    val name: String,       // Place name
    val image: String?,     // Place image (Base64 or URL)
    val isVisited: Boolean, // True if the place was visited
    val isSecret: Boolean = false // True if the place is secret
) {
    // Display name (??? if secret and not visited)
    val displayName: String
        get() = if (isSecret && !isVisited) "???" else name

    // Display image (null if secret and not visited)
    val displayImage: String?
        get() = if (isSecret && !isVisited) null else image
}
