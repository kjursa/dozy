package com.dozy.mobile.di

import android.content.Context
import com.dozy.api.client.auth.AuthSessionStorage
import com.dozy.api.client.auth.StoredAuthSession

internal class AndroidAuthSessionStorage(context: Context) : AuthSessionStorage {
    private val preferences = context.getSharedPreferences("dozy-auth", Context.MODE_PRIVATE)
    override suspend fun load(): StoredAuthSession? {
        val owner = preferences.getString(OWNER, null) ?: return null
        val access = preferences.getString(ACCESS, null) ?: return null
        val refresh = preferences.getString(REFRESH, null) ?: return null
        return StoredAuthSession(owner, access, refresh)
    }
    override suspend fun save(session: StoredAuthSession) {
        check(preferences.edit().putString(OWNER, session.ownerId).putString(ACCESS, session.accessToken).putString(REFRESH, session.refreshToken).commit())
    }
    override suspend fun clear() { check(preferences.edit().remove(OWNER).remove(ACCESS).remove(REFRESH).remove(ANONYMOUS).commit()) }
    override suspend fun anonymousOwnerId(): String? = preferences.getString(ANONYMOUS, null)
    override suspend fun saveAnonymousOwnerId(ownerId: String) { check(preferences.edit().putString(ANONYMOUS, ownerId).commit()) }
    override suspend fun clearAnonymousOwnerId() { check(preferences.edit().remove(ANONYMOUS).commit()) }

    private companion object {
        const val OWNER = "ownerId"
        const val ACCESS = "accessToken"
        const val REFRESH = "refreshToken"
        const val ANONYMOUS = "anonymousOwnerId"
    }
}
