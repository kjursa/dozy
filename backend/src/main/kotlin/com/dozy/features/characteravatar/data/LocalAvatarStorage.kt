package com.dozy.features.characteravatar.data

import com.dozy.config.AvatarConfig
import com.dozy.domain.id.AssetId
import com.dozy.features.characteravatar.domain.repository.AvatarStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LocalAvatarStorage(config: AvatarConfig) : AvatarStorage {
    private val root = Path.of(config.storagePath).toAbsolutePath()
    override suspend fun put(id: AssetId, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val target = path(id)
        Files.createDirectories(root)
        if (!Files.exists(target)) {
            val temporary = Files.createTempFile(root, "avatar-", ".tmp")
            try {
                Files.write(temporary, bytes)
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } finally { Files.deleteIfExists(temporary) }
        }
        Unit
    }
    override suspend fun get(id: AssetId): ByteArray? = withContext(Dispatchers.IO) {
        path(id).takeIf(Files::isRegularFile)?.let(Files::readAllBytes)
    }

    private fun path(id: AssetId): Path = root.resolve("${id.value}.png")
}
