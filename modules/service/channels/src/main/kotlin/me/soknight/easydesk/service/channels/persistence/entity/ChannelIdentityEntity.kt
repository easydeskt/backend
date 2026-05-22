package me.soknight.easydesk.service.channels.persistence.entity

import kotlin.time.Instant
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity
import me.soknight.easydesk.service.channels.persistence.table.ChannelIdentitiesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class ChannelIdentityEntity(id: EntityID<Long>) : LongEntity(id) {

    var channelBrand : String by ChannelIdentitiesTable.channelBrand
    var displayName  : String? by ChannelIdentitiesTable.displayName
    var firstSeenAt  : Instant by ChannelIdentitiesTable.firstSeenAt
    var lastSeenAt   : Instant by ChannelIdentitiesTable.lastSeenAt
    var nativeId     : String by ChannelIdentitiesTable.nativeId

    fun toDomain(provider: ChannelProvider) = ChannelIdentity(
        channelProvider = provider,
        displayName = displayName,
        firstSeenAt = firstSeenAt,
        identifier = id.value,
        lastSeenAt = lastSeenAt,
        nativeId = nativeId,
    )

    companion object : LongEntityClass<ChannelIdentityEntity>(ChannelIdentitiesTable)

}
