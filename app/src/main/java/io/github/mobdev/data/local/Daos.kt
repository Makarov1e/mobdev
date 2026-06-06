package io.github.mobdev.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE channel = :channel ORDER BY serverId ASC")
    fun observe(channel: String): Flow<List<CachedMessage>>

    @Query("SELECT MIN(serverId) FROM messages WHERE channel = :channel")
    suspend fun oldestServerId(channel: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(messages: List<CachedMessage>)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY position ASC")
    fun observe(): Flow<List<CachedChannel>>

    @Query("DELETE FROM channels")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<CachedChannel>)

    @Transaction
    suspend fun replaceAll(channels: List<CachedChannel>) {
        clear()
        insertAll(channels)
    }
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE channel = :channel ORDER BY localId ASC")
    fun observe(channel: String): Flow<List<OutboxMessage>>

    @Query("SELECT * FROM outbox ORDER BY localId ASC")
    suspend fun all(): List<OutboxMessage>

    @Insert
    suspend fun insert(message: OutboxMessage)

    @Delete
    suspend fun delete(message: OutboxMessage)
}
