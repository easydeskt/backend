package me.soknight.easydesk.service.channels.persistence.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

internal object ChannelIdentitiesTable : LongIdTable("channel_identities") {

    val channelBrand = varchar("channel_brand", 16)
    val displayName = varchar("display_name", 256).nullable()
    val firstSeenAt = timestamp("first_seen_at")
    val lastSeenAt = timestamp("last_seen_at")
    val nativeId = varchar("native_id", 256)

}
