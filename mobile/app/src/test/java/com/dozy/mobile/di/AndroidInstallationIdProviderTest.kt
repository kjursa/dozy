package com.dozy.mobile.di

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AndroidInstallationIdProviderTest {
    @Test
    fun reusesTheInstallationUuidAcrossProviderRecreation() {
        val context: Context = RuntimeEnvironment.getApplication()
        context
            .getSharedPreferences("dozy-installation", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val first = AndroidInstallationIdProvider(context).get()
        assertEquals(UUID.fromString(first).toString(), first)
        assertEquals(first, AndroidInstallationIdProvider(context).get())
    }
}
