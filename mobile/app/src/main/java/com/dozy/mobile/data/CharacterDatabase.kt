package com.dozy.mobile.data

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import com.dozy.mobile.domain.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val personality: String,
    val appearance: String,
    val type: String,
    val pendingSync: Boolean,
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE, id")
    fun observe(): Flow<List<CharacterEntity>>

    @Upsert
    suspend fun upsert(character: CharacterEntity)
}

@Database(entities = [CharacterEntity::class], version = 1, exportSchema = true)
abstract class CharacterDatabase : RoomDatabase() {
    abstract fun characters(): CharacterDao
}

fun Character.toEntity(pendingSync: Boolean) =
    CharacterEntity(
        id.value,
        details.name.value,
        details.personality.value,
        details.appearance.value,
        requireNotNull(details.type).name,
        pendingSync,
    )

fun CharacterEntity.toDomain() =
    Character(
        CharacterId(id),
        CharacterDraft(
            CharacterName.from(name),
            CharacterPersonality.from(personality),
            CharacterAppearance.from(appearance),
            CharacterType.valueOf(type),
        ),
    )
