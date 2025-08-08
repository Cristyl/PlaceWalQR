package com.example.placewalqr

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("api/getPlacesByUser")
    suspend fun getPlacesByUser(@Query("user_nickname") nickname: String): Response<List<Place>>

    // Nuovo endpoint per la leaderboard
    @GET("api/getLeaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntry>>

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<User>

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/visitPlaceById")
    //suspend fun visitPlaceById(@Query("place_id") place_id: Int, @Query("user_email") user_email: String, @Query("date_of_visit") date_of_visit: String): Response<VisitPlaceResponse>
    suspend fun visitPlaceById(@Body request: VisitPlaceRequest): Response<VisitPlaceResponse>

}
