package com.dozy.mobile

import android.app.Application
import com.dozy.mobile.di.androidModule
import com.dozy.mobile.feature.character.di.characterModule
import com.dozy.mobile.feature.home.di.homeModule
import com.dozy.mobile.feature.place.di.placeModule
import com.dozy.mobile.feature.story.di.storyModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DozyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DozyApplication)
            modules(placeModule, characterModule, homeModule, storyModule, androidModule)
        }
    }
}
