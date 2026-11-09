package com.dozy.mobile

import android.app.Application
import com.dozy.mobile.di.androidModule
import com.dozy.mobile.di.characterModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DozyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DozyApplication)
            modules(characterModule, androidModule)
        }
    }
}
