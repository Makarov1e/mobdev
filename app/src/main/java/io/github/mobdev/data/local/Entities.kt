package io.github.mobdev.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Кэшированное сообщение канала. Первичный ключ составной (channel + id),
 * поэтому повторная вставка того же сообщения просто заменяет запись —
 * дубликаты в БД невозможны.
 */
@Entity(tableName = "messages", primaryKeys = ["channel", "id"])
data class CachedMessage(
    val channel: String,
    val id: String,
    val from: String,
    val text: String?,
    val imageLink: String?,
    val time: String?,
    val serverId: Long
)

/** Кэшированный список каналов (для показа без сети). */
@Entity(tableName = "channels")
data class CachedChannel(
    @PrimaryKey val name: String,
    val position: Int
)

/**
 * Исходящее сообщение, ещё не доставленное на сервер.
 * Хранится в постоянном хранилище и отправляется автоматически,
 * как только появляется сеть.
 */
@Entity(tableName = "outbox")
data class OutboxMessage(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val channel: String,
    val from: String,
    val text: String,
    val createdAt: Long
)
