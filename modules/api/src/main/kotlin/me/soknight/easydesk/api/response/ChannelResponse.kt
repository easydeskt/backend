package me.soknight.easydesk.api.response

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.soknight.easydesk.channel.api.ChannelProviderRegistry
import me.soknight.easydesk.channel.api.config.ConfigField
import me.soknight.easydesk.channel.api.config.ConfigSchema
import me.soknight.easydesk.service.channels.data.domain.Channel

@Serializable
data class ChannelResponse(
    val brand: String,
    val config: JsonObject?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("display_name") val displayName: String,
    val id: Long,
    @SerialName("is_enabled") val isEnabled: Boolean,
)

fun Channel.toResponse(providerRegistry: ChannelProviderRegistry? = null) = ChannelResponse(
    brand = brand,
    config = providerRegistry?.getOrNull(brand)?.configSchema?.let { schema -> config.maskPasswords(schema) } ?: config,
    createdAt = createdAt,
    displayName = displayName,
    id = id,
    isEnabled = isEnabled,
)

private fun JsonObject.maskPasswords(schema: ConfigSchema): JsonObject = buildJsonObject {
    for ((key, value) in this@maskPasswords) {
        val field = schema[key] ?: schema.sections.firstNotNullOfOrNull { it[key] }
        put(key, if (field is ConfigField.Text.Password) JsonPrimitive("***") else value)
    }
}
