package io.github.mobdev.api

import io.github.mobdev.api.models.LoginRequest
import io.github.mobdev.api.models.Message
import io.github.mobdev.api.models.SendMessageRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): String

    @GET("channels")
    suspend fun getChannels(): List<String>

    @GET("channel/{channelName}")
    suspend fun getMessages(
        @Path("channelName") channelName: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String? = null,
        @Query("reverse") reverse: Boolean = true
    ): List<Message>

    @POST("messages")
    suspend fun sendMessage(@Body message: SendMessageRequest): String

    @POST("logout")
    suspend fun logout()
}
