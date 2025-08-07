package com.example.placewalqr

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("api/getPlacesByUser")
    suspend fun getPlacesByUser(@Query("user_nickname") nickname: String): Response<List<Place>>

    // Nuovo endpoint per la leaderboard
    @GET("api/getLeaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntry>>
}
