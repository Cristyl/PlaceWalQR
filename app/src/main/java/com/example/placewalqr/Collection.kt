// Data classes for collections
package com.example.placewalqr

data class Collection(
    val id: Int,
    val name: String,
    val image: String?, // image (Base64 or URL)
    val isUnknown: Boolean, // hidden collection
    val points: Int,
    val totalPlaces: Int = 0, // total places
    val visitedPlaces: Int = 0 // visited places
) {
    // name to show (??? if hidden and not completed)
    val displayName: String
        get() = if (isUnknown && visitedPlaces < totalPlaces) "???" else name

    // image to show (null if hidden and not completed)
    val displayImage: String?
        get() = if (isUnknown && visitedPlaces < totalPlaces) null else image

    // check if completed
    val isCompleted: Boolean
        get() = visitedPlaces >= totalPlaces

    // progress as "x/y"
    val progressText: String
        get() = "$visitedPlaces/$totalPlaces"
}
