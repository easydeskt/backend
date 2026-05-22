package me.soknight.easydesk.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.channel.api.ChannelProvider

@Serializable
data class ChannelProviderResponse(
    @SerialName("brand") val brand: String,
    @SerialName("config") val configSchema: ConfigSchemaResponse,
    @SerialName("name") val name: String,
)

fun ChannelProvider.toResponse() = ChannelProviderResponse(
    brand = brand.identifier,
    configSchema = configSchema.toResponse(),
    name = brand.humanName,
)
