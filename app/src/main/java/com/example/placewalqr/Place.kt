package com.example.placewalqr

import android.util.Base64

// Data class for a Place
data class Place(
    val name: String,          // Place name
    val imageBase64: String?   // Image in Base64 format
) {
    // Convert Base64 image to byte array
    fun getImageBytes(): ByteArray? {
        return if (!imageBase64.isNullOrEmpty()) {
            Base64.decode(imageBase64.replace("\n", ""), Base64.DEFAULT) // Decode image
        } else {
            null // No image available
        }
    }
}