package com.example.placewalqr

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // endopoint per caricare i posti visitati
    @GET("api/getPlacesByUser")
    suspend fun getPlacesByUser(@Query("user_id") id: String): Response<List<Place>>

    // endpoint per la leaderboard
    @GET("api/getLeaderboard")
    suspend fun getLeaderboard(@Query("user_nickname") nickname: String): Response<List<LeaderboardEntry>>

    // endpoint per login
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<User>

    // endpoint per regitrazione
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // endpoint per visitare luogo
    @POST("api/visitPlaceById")
    //suspend fun visitPlaceById(@Query("place_id") place_id: Int, @Query("user_email") user_email: String, @Query("date_of_visit") date_of_visit: String): Response<VisitPlaceResponse>
    suspend fun visitPlaceById(@Body request: VisitPlaceRequest): Response<VisitPlaceResponse>

    // endpoint per salvare foto ricordo
    @POST("api/saveSouvenir")
    suspend fun saveSouvenir(@Body request: SouvenirRequest): Response<SouvenirResponse>

    @GET("api/findUserByEmail")
    suspend fun findUserByEmail()

    @GET("api/getPointsById/{id}")
    suspend fun getPointsById(@Path("id") user_id: Int): Response<PointsByIdResponse>

    @GET("api/findAllPlaces")
    suspend fun findAllPlaces(): Response<List<Place>>

    @GET("api/findAllPlacesByUser")
    suspend fun findAllPlacesByUser()

    @GET("api/findLastPlaceById/{id}")
    suspend fun findLastPlaceById(@Path("id") user_id: Int): Response<LastPlaceResponse>

    @GET("api/findVisitedPlaceById/{id}")
    suspend fun findVisitedPlaceById(@Path("id") user_id: Int): Response<List<VisitedPlaceResponse>>
}
