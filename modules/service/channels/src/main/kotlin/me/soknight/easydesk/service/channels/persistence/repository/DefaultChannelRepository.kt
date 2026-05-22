package me.soknight.easydesk.service.channels.persistence.repository

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.EMPTY_JSON_OBJECT
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.channels.data.domain.Channel
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import me.soknight.easydesk.service.channels.persistence.entity.ChannelEntity
import me.soknight.easydesk.service.channels.persistence.table.ChannelsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
internal class DefaultChannelRepository : ChannelRepository {

    override suspend fun create(
        brand: String,
        displayName: String,
        config: JsonObject?,
    ): Channel = suspendTransaction {
        val now = Clock.System.now()
        ChannelEntity.new {
            this.attributes = EMPTY_JSON_OBJECT
            this.brand = brand
            this.config = config ?: EMPTY_JSON_OBJECT
            this.createdAt = now
            this.displayName = displayName
            this.isEnabled = true
            this.updatedAt = now
        }
    }.toDomain()

    override suspend fun findAll(enabledOnly: Boolean): List<Channel> =
        suspendTransaction {
            if (enabledOnly) {
                ChannelEntity.find { ChannelsTable.isEnabled eq true }
            } else {
                ChannelEntity.all()
            }.map(ChannelEntity::toDomain)
        }

    override suspend fun findById(id: Long): Channel? =
        suspendTransaction { ChannelEntity.findById(id) }?.toDomain()

    override suspend fun findByBrand(brand: String, enabledOnly: Boolean): List<Channel> =
        suspendTransaction {
            with(ChannelsTable) {
                if (enabledOnly) {
                    ChannelEntity.find { (this.brand eq brand) and (isEnabled eq true) }
                } else {
                    ChannelEntity.find { this.brand eq brand }
                }.map(ChannelEntity::toDomain)
            }
        }

    override suspend fun update(id: Long, displayName: String?, config: JsonObject?, isEnabled: Boolean?): Channel? =
        suspendTransaction {
            val entity = ChannelEntity.findById(id) ?: return@suspendTransaction null
            displayName?.let { entity.displayName = it }
            config?.let { entity.config = it }
            isEnabled?.let { entity.isEnabled = it }
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

}
