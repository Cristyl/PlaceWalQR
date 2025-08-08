package com.example.placewalqr

import java.util.Date

data class User(
    val name: String,
    val surname: String,
    val nickname: String,
    val dob: Date,
    val email: String
)