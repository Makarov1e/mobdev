package io.github.mobdev

import android.app.Application
import io.github.mobdev.data.Graph

class MobdevApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
