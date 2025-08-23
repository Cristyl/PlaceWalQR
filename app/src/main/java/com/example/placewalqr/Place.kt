package com.example.placewalqr

import android.util.Base64

data class Place(
    val name: String,
    val imageBase64: String?
) {
    fun getImageBytes(): ByteArray? {
        return if (!imageBase64.isNullOrEmpty()) {
            Base64.decode(imageBase64.replace("\n", ""), Base64.DEFAULT)
        } else {
            null
        }
    }
}

