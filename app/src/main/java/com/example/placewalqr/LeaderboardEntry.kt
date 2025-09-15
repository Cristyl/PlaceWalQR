package com.example.placewalqr

// Model for a leaderboard entry
data class LeaderboardEntry(
    val position: Int,      // Rank position
    val nickname: String,   // User nickname
    val total_points: Int   // User total points
)
