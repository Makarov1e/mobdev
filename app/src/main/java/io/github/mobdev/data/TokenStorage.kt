package io.github.mobdev.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var username: String?
        get() = prefs.getString("username", null)
        set(value) = prefs.edit { putString("username", value) }

    var password: String?
        get() = prefs.getString("password", null)
        set(value) = prefs.edit { putString("password", value) }

    fun clear() = prefs.edit { clear() }
}
