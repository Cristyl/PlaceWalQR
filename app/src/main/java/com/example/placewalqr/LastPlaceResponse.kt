package com.example.placewalqr

import android.util.Log

data class LastPlaceResponse(
    val status: String,
    val place_id: Int,
    val name: String,
    val information: String,
    val point: Int,
    val date: String,
    val image: String?
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
