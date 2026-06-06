package io.github.mobdev.data

import android.content.Context
import io.github.mobdev.api.RetrofitClient
import io.github.mobdev.data.local.AppDatabase

/** Простой сервис-локатор: единые экземпляры репозитория и наблюдателя сети. */
object Graph {

    lateinit var repository: ChatRepository
        private set

    lateinit var connectivity: ConnectivityObserver
        private set

    fun init(context: Context) {
        val db = AppDatabase.get(context)
        repository = ChatRepository(
            api = RetrofitClient.apiService,
            messageDao = db.messageDao(),
            channelDao = db.channelDao(),
            outboxDao = db.outboxDao()
        )
        connectivity = ConnectivityObserver(context)
    }
}
