package com.dozy.features.characteravatar.data.repository

import com.dozy.features.characteravatar.data.database.Jobs
import com.dozy.config.AvatarConfig
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.model.AvatarJob
import com.dozy.features.characteravatar.domain.model.AvatarState
import com.dozy.features.characteravatar.domain.repository.AvatarJobs
import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.infrastructure.database.SqliteDatabase
import java.util.UUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class SqliteAvatarJobs(private val database: SqliteDatabase, private val config: AvatarConfig) : AvatarJobs {
    override suspend fun create(owner: InstallationId, input: AvatarInput): AvatarJob = database.write {
        registerInstallation(owner)
        val id = uuid7()
        val asset = uuid7()
        val job = AvatarJob(id, owner.value, input, asset, "${config.publicBaseUrl.trimEnd('/')}/assets/$asset", AvatarState.QUEUED)
        Jobs.insert {
            it[Jobs.id] = id
            it[Jobs.owner] = owner.value
            it[type] = input.type
            it[personality] = input.personality
            it[appearance] = input.appearance
            it[assetId] = asset
            it[url] = job.url
            it[state] = job.state.name
        }
        job
    }
    override suspend fun find(owner: InstallationId, id: String): AvatarJob? = database.write {
        Jobs.selectAll().where { (Jobs.id eq id) and (Jobs.owner eq owner.value) }.singleOrNull()?.toJob()
    }
    override suspend fun recover() {
        database.write {
            Jobs.update({ Jobs.state eq AvatarState.RUNNING.name }) { it[state] = AvatarState.QUEUED.name }
        }
    }
    override suspend fun claim(): AvatarJob? = database.write {
        val job = Jobs.selectAll().where { Jobs.state eq AvatarState.QUEUED.name }.limit(1).singleOrNull()?.toJob() ?: return@write null
        Jobs.update({ Jobs.id eq job.id }) { it[state] = AvatarState.RUNNING.name }
        job.copy(state = AvatarState.RUNNING)
    }
    override suspend fun complete(job: AvatarJob, success: Boolean) {
        database.write {
            val current = Jobs.selectAll().where { Jobs.id eq job.id }.single().toJob()
            if (current.state != AvatarState.RUNNING) return@write
            if (success) {
                Assets.insert {
                    it[id] = job.assetId
                    it[installationId] = job.owner
                    it[url] = job.url
                    it[status] = "READY"
                }
            }
            Jobs.update({ Jobs.id eq job.id }) {
                it[state] = if (success) AvatarState.READY.name else AvatarState.FAILED.name
            }
        }
    }
}
private fun ResultRow.toJob(): AvatarJob {
    val input = AvatarInput.parse(this[Jobs.type], this[Jobs.personality], this[Jobs.appearance]) as AvatarInput.Result.Valid
    return AvatarJob(this[Jobs.id], this[Jobs.owner], input.input, this[Jobs.assetId], this[Jobs.url], AvatarState.valueOf(this[Jobs.state]))
}
internal fun uuid7(): String {
    val random = UUID.randomUUID()
    return UUID((System.currentTimeMillis() shl 16) or 0x7000L or (random.mostSignificantBits and 0xfffL), random.leastSignificantBits).toString()
}
