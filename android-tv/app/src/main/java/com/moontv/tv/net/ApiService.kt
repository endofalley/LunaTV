package com.moontv.tv.net

import retrofit2.http.*

interface ApiService {
    @POST("/api/login/token")
    suspend fun loginToken(@Body body: Map<String, String>): TokenResponse

    @GET("/api/detail")
    suspend fun getDetail(
        @Query("id") id: String,
        @Query("source") source: String
    ): Detail

    @GET("/api/skipconfigs")
    suspend fun getSkipConfig(
        @Query("source") source: String,
        @Query("id") id: String
    ): SkipConfig?

    @GET("/api/playrecords")
    suspend fun getAllPlayRecords(): Map<String, PlayRecord>

    @POST("/api/playrecords")
    suspend fun savePlayRecord(@Body body: SaveRecordRequest): Map<String, Any>

    // Favorites
    @GET("/api/favorites")
    suspend fun getAllFavorites(): Map<String, Favorite>

    @POST("/api/favorites")
    suspend fun saveFavorite(@Body body: SaveFavoriteRequest): Map<String, Any>

    @DELETE("/api/favorites")
    suspend fun deleteFavorite(@Query("key") key: String? = null): Map<String, Any>

    // Live
    @GET("/api/live/sources")
    suspend fun getLiveSources(): LiveSourcesResponse

    @GET("/api/live/channels")
    suspend fun getLiveChannels(@Query("source") source: String): LiveChannelsResponse

    @GET("/api/live/epg")
    suspend fun getLiveEpg(
        @Query("source") source: String,
        @Query("tvgId") tvgId: String
    ): LiveEpgResponse

    // Search
    @GET("/api/search")
    suspend fun search(@Query("q") q: String): SearchResponse
}
