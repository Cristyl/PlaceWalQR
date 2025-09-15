package com.example.placewalqr

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // endopoint for the places visited by a user
    @GET("api/getPlacesByUser")
    suspend fun getPlacesByUser(@Query("user_id") id: String): Response<List<Place>>

    // endpoint per la leaderboard
    @GET("api/getLeaderboard")
    suspend fun getLeaderboard(@Query("user_nickname") nickname: String): Response<List<LeaderboardEntry>>

    // login endpoint
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<User>

    // registration endpoint
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // google signin endpoint
    @POST("api/googleAuth")
    suspend fun googleAuth(@Body userData: GoogleUserData): Response<GoogleAuthResponse>

    // place visit endpoint
    @POST("api/visitPlaceById")
    suspend fun visitPlaceById(@Body request: VisitPlaceRequest): Response<VisitPlaceResponse>

    // souvenir photo taken endpoint
    @POST("api/saveSouvenir")
    suspend fun saveSouvenir(@Body request: SouvenirRequest): Response<SouvenirResponse>

    // endpoint for points of a user
    @GET("api/getPointsById/{id}")
    suspend fun getPointsById(@Path("id") user_id: Int): Response<PointsByIdResponse>

    // endpoint for finding last place visited by a user
    @GET("api/findLastPlaceById/{id}")
    suspend fun findLastPlaceById(@Path("id") user_id: Int): Response<LastPlaceResponse>

    // endopoint for the places visited by a user
    @GET("api/findVisitedPlaceById/{id}")
    suspend fun findVisitedPlaceById(@Path("id") user_id: Int): Response<List<VisitedPlaceResponse>>

    // endpoint for finding collections of a user
    @GET("api/collections/{userId}")
    suspend fun getUserCollections(@Path("userId") userId: String): Response<List<Collection>>

    // endpoint for getting places of a specific collection
    @GET("api/collection/{collectionId}/places/{userId}")
    suspend fun getCollectionPlaces(
        @Path("collectionId") collectionId: Int,
        @Path("userId") userId: String
    ): Response<CollectionDetailResponse>
}
