package com.dozy.mobile

import android.app.Application
import androidx.work.Configuration
import com.dozy.mobile.di.androidModule
import com.dozy.mobile.feature.character.di.characterModule
import com.dozy.mobile.feature.home.di.homeModule
import com.dozy.mobile.feature.place.di.placeModule
import com.dozy.mobile.feature.story.data.background.GenerationWorkScheduler
import com.dozy.mobile.feature.story.data.background.GenerationWorkerFactory
import com.dozy.mobile.feature.story.di.storyModule
import com.dozy.mobile.feature.story.domain.usecase.ObserveGenerations
import kotlinx.coroutines.*
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DozyApplication :
    Application(),
    Configuration.Provider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(get<GenerationWorkerFactory>()).build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DozyApplication)
            modules(placeModule, characterModule, homeModule, storyModule, androidModule)
        }
        val scheduler = get<GenerationWorkScheduler>()
        scheduler.resume()
        scope.launch {
            get<ObserveGenerations>()().collect { scheduler.checkNow() }
        }
    }
}
