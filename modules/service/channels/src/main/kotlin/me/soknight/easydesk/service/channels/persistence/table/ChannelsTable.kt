package me.soknight.easydesk.service.channels.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

internal object ChannelsTable : LongIdTable("channels") {

    val attributes = jsonb<JsonObject>("attributes", Json)
    val brand = varchar("brand", 16)
    val config = jsonb<JsonObject>("config", Json)
    val createdAt = timestamp("created_at")
    val displayName = varchar("display_name", 128)
    val isEnabled = bool("is_enabled")
    val updatedAt = timestamp("updated_at")

}
