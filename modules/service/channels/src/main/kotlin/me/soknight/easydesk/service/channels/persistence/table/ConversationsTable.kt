package me.soknight.easydesk.service.channels.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

internal object ConversationsTable : LongIdTable("conversations") {

    val attributes = jsonb<JsonObject>("attributes", Json)
    val channelId = long("channel_id")
    val createdAt = timestamp("created_at")
    val identityId = long("identity_id")

}
