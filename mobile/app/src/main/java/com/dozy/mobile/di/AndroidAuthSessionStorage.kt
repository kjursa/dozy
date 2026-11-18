package com.dozy.mobile.di

import android.content.Context
import com.dozy.api.client.auth.AuthSessionStorage
import com.dozy.api.client.auth.StoredAuthSession

internal class AndroidAuthSessionStorage(
    context: Context,
) : AuthSessionStorage {
    private val preferences = context.getSharedPreferences("dozy-auth", Context.MODE_PRIVATE)

    override suspend fun load(): StoredAuthSession? {
        val owner = preferences.getString("ownerId", null) ?: return null
        val access = preferences.getString("accessToken", null) ?: return null
        val refresh = preferences.getString("refreshToken", null) ?: return null
        return StoredAuthSession(owner, access, refresh, preferences.getBoolean("guest", false))
    }

    override suspend fun save(session: StoredAuthSession) {
        check(
            preferences
                .edit()
                .putString("ownerId", session.ownerId)
                .putString("accessToken", session.accessToken)
                .putString("refreshToken", session.refreshToken)
                .putBoolean("guest", session.anonymous)
                .commit(),
        )
    }

    override suspend fun clear() {
        check(preferences.edit().clear().commit())
    }
}
