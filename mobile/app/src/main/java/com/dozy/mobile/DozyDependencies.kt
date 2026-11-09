package com.dozy.mobile

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dozy.mobile.data.CharacterDatabase
import com.dozy.mobile.data.DefaultCharacterRepository
import com.dozy.mobile.data.KtorCharacterApi
import com.dozy.mobile.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.util.UUID

class DozyApplication : Application() {
    internal val viewModelFactory: ViewModelProvider.Factory by lazy {
        val database =
            Room
                .databaseBuilder<CharacterDatabase>(this, "characters.db")
                .setDriver(BundledSQLiteDriver())
                .build()
        val client =
            HttpClient(Android) {
                install(ContentNegotiation) { json() }
                install(HttpTimeout) { requestTimeoutMillis = 5000 }
            }
        // No character endpoint exists in the backend yet. Enable only when its contract is agreed.
        val repository = DefaultCharacterRepository(database.characters(), KtorCharacterApi(client, baseUrl = null))
        DozyViewModelFactory(
            SaveCharacter(repository, CharacterIdGenerator { CharacterId(UUID.randomUUID().toString()) }),
            ObserveCharacters(repository),
        )
    }
}

internal val LocalDozyViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> { error("Missing Dozy dependencies") }

internal class DozyViewModelFactory(
    private val saveCharacter: SaveCharacter,
    private val observeCharacters: ObserveCharacters,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T =
        when (modelClass) {
            CreateCharacterViewModel::class.java -> CreateCharacterViewModel(extras.createSavedStateHandle(), saveCharacter)
            HomeViewModel::class.java -> HomeViewModel(observeCharacters)
            CharactersViewModel::class.java -> CharactersViewModel(observeCharacters)
            else -> error("Unknown ViewModel: $modelClass")
        } as T
}
