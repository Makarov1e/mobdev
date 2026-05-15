package io.github.mobdev.data

import io.github.mobdev.api.ChatApiService
import io.github.mobdev.api.models.LoginRequest
import io.github.mobdev.api.models.MessageData
import io.github.mobdev.api.models.SendMessageRequest
import io.github.mobdev.api.models.TextContent
import retrofit2.HttpException

class ChatRepository(private val api: ChatApiService) {

    suspend fun login(name: String, password: String): Result<String> = runCatching {
        api.login(LoginRequest(name, password)).trim()
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            if (e is HttpException && e.code() == 401)
                Result.Error("Неверный логин или пароль", isUnauthorized = true)
            else
                Result.Error(e.message ?: "Ошибка сети")
        }
    )

    suspend fun getChannels(): Result<List<String>> = runCatching {
        api.getChannels()
    }.toResult()

    suspend fun getMessages(
        channelName: String,
        limit: Int = 20,
        lastKnownId: String? = null,
        reverse: Boolean = true
    ): Result<List<io.github.mobdev.api.models.Message>> = runCatching {
        api.getMessages(channelName, limit, lastKnownId, reverse)
    }.toResult()

    suspend fun sendMessage(from: String, to: String, text: String): Result<String> = runCatching {
        api.sendMessage(
            SendMessageRequest(
                from = from,
                to = to,
                data = MessageData(text = TextContent(text), image = null)
            )
        )
    }.toResult()

    suspend fun logout() = runCatching { api.logout() }

    private fun <T> kotlin.Result<T>.toResult(): Result<T> = fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            if (e is HttpException)
                Result.Error("Ошибка сервера: ${e.code()}", isUnauthorized = e.code() == 401)
            else
                Result.Error(e.message ?: "Ошибка сети")
        }
    )
}
