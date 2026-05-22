package me.soknight.easydesk.service.channels.persistence.entity

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.channels.data.domain.Channel
import me.soknight.easydesk.service.channels.persistence.table.ChannelsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import kotlin.time.Instant

internal class ChannelEntity(id: EntityID<Long>) : LongEntity(id) {

    var attributes  : JsonObject by ChannelsTable.attributes
    var brand       : String by ChannelsTable.brand
    var config      : JsonObject by ChannelsTable.config
    var createdAt   : Instant by ChannelsTable.createdAt
    var displayName : String by ChannelsTable.displayName
    var isEnabled   : Boolean by ChannelsTable.isEnabled
    var updatedAt   : Instant by ChannelsTable.updatedAt

    fun toDomain() = Channel(
        attributes = attributes,
        brand = brand,
        config = config,
        createdAt = createdAt,
        displayName = displayName,
        id = id.value,
        isEnabled = isEnabled,
        updatedAt = updatedAt,
    )

    companion object : LongEntityClass<ChannelEntity>(ChannelsTable)

}
