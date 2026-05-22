package me.soknight.easydesk.channel.vkontakte.vk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

data class VkLongPollServer(
    val key: String,
    val server: String,
    val ts: String,
)

@Serializable
internal data class VkLongPollResponse(
    val failed: Int? = null,
    val key: String? = null,
    val server: String? = null,
    val ts: String? = null,
    val updates: List<VkRawUpdate> = emptyList(),
)

@Serializable
internal data class VkRawUpdate(
    @SerialName("object") val obj: JsonObject,
    val type: String,
)
