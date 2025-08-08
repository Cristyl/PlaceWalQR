package com.example.placewalqr

data class RegisterRequest(
    val name: String,
    val surname: String,
    val nickname: String,
    val dob: String,
    val email: String,
    val password: String
)
