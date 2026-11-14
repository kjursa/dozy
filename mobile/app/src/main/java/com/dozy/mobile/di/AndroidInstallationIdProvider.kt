package com.dozy.mobile.di

import android.content.Context
import com.dozy.api.client.InstallationIdProvider
import java.util.UUID

internal class AndroidInstallationIdProvider(
    context: Context,
) : InstallationIdProvider {
    private val preferences = context.getSharedPreferences("dozy-installation", Context.MODE_PRIVATE)
    private val id =
        preferences.getString("id", null) ?: UUID.randomUUID().toString().also {
            check(preferences.edit().putString("id", it).commit())
        }

    override fun get(): String = id
}
