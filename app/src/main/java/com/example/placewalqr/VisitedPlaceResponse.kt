package com.example.placewalqr

import android.util.Log

data class VisitedPlaceResponse(
    val name: String,
    val information: String,
    val latitude: Double,
    val longitude: Double,
    val id: Int,
    val image: String?,
    val visited: Boolean
){
    fun getImageBytes(): ByteArray? {
        return if (image != null && image.isNotEmpty()) {
            try {
                android.util.Base64.decode(image, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e("ImageDecode", "Error decoding base64: ${e.message}")
                null
            }
        } else {
            null
        }
    }
}
