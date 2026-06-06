package io.github.mobdev.data

import io.github.mobdev.api.ChatApiService
import io.github.mobdev.api.models.LoginRequest
import io.github.mobdev.api.models.Message
import io.github.mobdev.api.models.MessageData
import io.github.mobdev.api.models.SendMessageRequest
import io.github.mobdev.api.models.TextContent
import io.github.mobdev.data.local.CachedChannel
import io.github.mobdev.data.local.CachedMessage
import io.github.mobdev.data.local.ChannelDao
import io.github.mobdev.data.local.MessageDao
import io.github.mobdev.data.local.OutboxDao
import io.github.mobdev.data.local.OutboxMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class ChatRepository(
    private val api: ChatApiService,
    private val messageDao: MessageDao,
    private val channelDao: ChannelDao,
    private val outboxDao: OutboxDao
) {

    // --- Авторизация ---

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

    suspend fun logout() = runCatching { api.logout() }

    // --- Каналы ---

    /** Поток каналов из локального кэша — отображается даже без сети. */
    fun channelsStream(): Flow<List<String>> = channelDao.observe().map { it.map(CachedChannel::name) }

    /** Загружает каналы из сети и обновляет кэш. */
    suspend fun refreshChannels(): Result<Unit> = runCatching {
        val channels = api.getChannels()
        channelDao.replaceAll(channels.mapIndexed { index, name -> CachedChannel(name, index) })
    }.toResult()

    // --- Сообщения ---

    /** Поток сообщений канала из локального кэша. */
    fun messagesStream(channel: String): Flow<List<Message>> =
        messageDao.observe(channel).map { list -> list.map(CachedMessage::toDomain) }

    /** Подгружает свежие сообщения канала с сервера и кладёт их в кэш. */
    suspend fun refreshLatest(channel: String, limit: Int = 20): Result<Unit> = runCatching {
        val fresh = api.getMessages(channel, limit = limit, reverse = true)
        messageDao.upsert(fresh.map { it.toCached(channel) })
    }.toResult()

    /** Подгружает более старые сообщения (пагинация вверх). */
    suspend fun loadOlder(channel: String, limit: Int = 20): Result<Int> = runCatching {
        val oldest = messageDao.oldestServerId(channel)?.toString()
        val older = api.getMessages(channel, limit = limit, lastKnownId = oldest, reverse = true)
        messageDao.upsert(older.map { it.toCached(channel) })
        older.size
    }.toResult()

    // --- Исходящая очередь (outbox) ---

    /** Поток сообщений, ожидающих отправки. */
    fun outboxStream(channel: String): Flow<List<OutboxMessage>> = outboxDao.observe(channel)

    /** Кладёт сообщение в постоянную очередь на отправку. */
    suspend fun enqueue(from: String, channel: String, text: String) {
        outboxDao.insert(
            OutboxMessage(
                channel = channel,
                from = from,
                text = text,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Пытается отправить все накопленные сообщения. При сетевой ошибке останавливается
     * (останется в очереди до следующей попытки). Возвращает [Result.Error] с
     * isUnauthorized при истёкшем токене.
     */
    suspend fun flushOutbox(): Result<Unit> {
        for (item in outboxDao.all()) {
            val sent = runCatching {
                api.sendMessage(
                    SendMessageRequest(
                        from = item.from,
                        to = item.channel,
                        data = MessageData(text = TextContent(item.text), image = null)
                    )
                )
            }
            sent.fold(
                onSuccess = { outboxDao.delete(item) },
                onFailure = { e ->
                    return if (e is HttpException && e.code() == 401)
                        Result.Error("Сессия истекла", isUnauthorized = true)
                    else
                        Result.Error(e.message ?: "Нет сети")
                }
            )
        }
        return Result.Success(Unit)
    }

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

private fun Message.toCached(channel: String) = CachedMessage(
    channel = channel,
    id = id,
    from = from,
    text = data.text?.text,
    imageLink = data.image?.link,
    time = time,
    serverId = id.toLongOrNull() ?: 0L
)

private fun CachedMessage.toDomain() = Message(
    id = id,
    from = from,
    to = channel,
    data = MessageData(
        text = text?.let { TextContent(it) },
        image = imageLink?.let { io.github.mobdev.api.models.ImageContent(it) }
    ),
    time = time
)
