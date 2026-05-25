package me.soknight.easydesk.channel.vkontakte.vk.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import me.soknight.easydesk.channel.vkontakte.vk.model.VkAttachment
import me.soknight.easydesk.channel.vkontakte.vk.model.VkClientInfo
import me.soknight.easydesk.channel.vkontakte.vk.model.VkGeo
import me.soknight.easydesk.channel.vkontakte.vk.model.VkLongPollServer
import me.soknight.easydesk.channel.vkontakte.vk.model.VkMessage
import me.soknight.easydesk.channel.vkontakte.vk.model.VkRawUpdate
import me.soknight.easydesk.channel.vkontakte.vk.model.VkUpdate
import me.soknight.easydesk.channel.vkontakte.vk.model.VkUser

// ── API response wrappers ─────────────────────────────────────────────────────

@Serializable
internal data class VkApiResponseWrapper<T>(
    val error: VkApiError? = null,
    val response: T? = null,
)

@Serializable
internal data class VkApiError(
    @SerialName("error_code") val code: Int,
    @SerialName("error_msg") val message: String,
)

// ── Long Poll server ──────────────────────────────────────────────────────────

@Serializable
internal data class LongPollServerDto(
    val key: String,
    val server: String,
    val ts: String,
)

internal fun LongPollServerDto.toDomain() = VkLongPollServer(key, server, ts)

// ── User ──────────────────────────────────────────────────────────────────────

@Serializable
internal data class VkUserDto(
    @SerialName("first_name") val firstName: String = "",
    val id: Long,
    @SerialName("last_name") val lastName: String = "",
    @SerialName("photo_200") val photo200: String? = null,
)

internal fun VkUserDto.toDomain() = VkUser(firstName, id, lastName, photo200)

// ── Message ───────────────────────────────────────────────────────────────────

@Serializable
internal data class VkMessageDto(
    val attachments: List<VkAttachmentDto> = emptyList(),
    @SerialName("conversation_message_id") val conversationMessageId: Int = 0,
    val date: Long = 0,
    @SerialName("from_id") val fromId: Long = 0,
    @SerialName("fwd_messages") val fwdMessages: List<VkMessageDto> = emptyList(),
    val geo: VkGeoDto? = null,
    val id: Int = 0,
    val out: Int = 0,
    @SerialName("peer_id") val peerId: Long = 0,
    @SerialName("reply_message") val replyMessage: VkMessageDto? = null,
    val text: String = "",
)

internal fun VkMessageDto.toDomain(): VkMessage = VkMessage(
    attachments = attachments.map { it.toDomain() },
    conversationMessageId = conversationMessageId,
    date = date,
    fromId = fromId,
    fwdMessages = fwdMessages.map { it.toDomain() },
    geo = geo?.toDomain(),
    id = id,
    isOut = out != 0,
    peerId = peerId,
    replyMessage = replyMessage?.toDomain(),
    text = text,
)

// ── Geo ───────────────────────────────────────────────────────────────────────

@Serializable
internal data class VkGeoDto(
    val coordinates: VkCoordinatesDto? = null,
    val type: String = "",
)

@Serializable
internal data class VkCoordinatesDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

internal fun VkGeoDto.toDomain() =
    VkGeo(coordinates?.let { VkGeo.Coordinates(it.latitude, it.longitude) }, type)

// ── Attachments ───────────────────────────────────────────────────────────────

@Serializable
internal data class VkAttachmentDto(
    @SerialName("audio_message") val audioMessage: VkAudioMessageDto? = null,
    val audio: VkAudioDto? = null,
    val doc: VkDocumentDto? = null,
    val graffiti: VkGraffitiDto? = null,
    val link: VkLinkDto? = null,
    val market: VkMarketDto? = null,
    val photo: VkPhotoDto? = null,
    val sticker: VkStickerDto? = null,
    val type: String,
    val video: VkVideoDto? = null,
    val wall: VkWallDto? = null,
)

internal fun VkAttachmentDto.toDomain(): VkAttachment = when (type) {
    "audio" -> audio?.let { VkAttachment.Audio(it.artist, it.id, it.ownerId, it.title) }
    "audio_message" -> audioMessage?.let { VkAttachment.AudioMessage(it.duration, it.id, it.linkMp3, it.ownerId) }
    "doc" -> doc?.let { VkAttachment.Document(it.ext, it.id, it.ownerId, it.title, it.url) }
    "graffiti" -> graffiti?.let { VkAttachment.Graffiti(it.id, it.ownerId, it.url) }
    "link" -> link?.let { VkAttachment.Link(it.caption, it.title, it.url) }
    "market" -> market?.let { VkAttachment.Market(it.id, it.ownerId, it.price?.text ?: "", it.title) }
    "photo" -> photo?.let {
        VkAttachment.Photo(
            id = it.id,
            ownerId = it.ownerId,
            sizes = it.sizes.map { s -> VkAttachment.Photo.Size(s.height, s.type, s.url, s.width) },
        )
    }
    "sticker" -> sticker?.let {
        VkAttachment.Sticker(
            images = it.images.map { img -> VkAttachment.Sticker.Image(img.height, img.url, img.width) },
            stickerId = it.stickerId,
        )
    }
    "video" -> video?.let { VkAttachment.Video(it.id, it.ownerId, it.title) }
    "wall" -> wall?.let { VkAttachment.Wall(it.id, it.ownerId, it.text) }
    else -> null
} ?: VkAttachment.Unknown(buildJsonObject {}, type)

@Serializable
internal data class VkPhotoDto(
    val id: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    val sizes: List<VkPhotoSizeDto> = emptyList(),
)

@Serializable
internal data class VkPhotoSizeDto(
    val height: Int = 0,
    val type: String = "",
    val url: String = "",
    val width: Int = 0,
)

@Serializable
internal data class VkDocumentDto(
    val ext: String = "",
    val id: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    val title: String = "",
    val url: String? = null,
)

@Serializable
internal data class VkAudioMessageDto(
    val duration: Int = 0,
    val id: Int = 0,
    @SerialName("link_mp3") val linkMp3: String = "",
    @SerialName("owner_id") val ownerId: Long = 0,
)

@Serializable
internal data class VkStickerDto(
    val images: List<VkStickerImageDto> = emptyList(),
    @SerialName("sticker_id") val stickerId: Int = 0,
)

@Serializable
internal data class VkStickerImageDto(
    val height: Int = 0,
    val url: String = "",
    val width: Int = 0,
)

@Serializable
internal data class VkAudioDto(
    val artist: String = "",
    val id: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    val title: String = "",
)

@Serializable
internal data class VkVideoDto(
    val id: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    val title: String = "",
)

@Serializable
internal data class VkLinkDto(
    val caption: String? = null,
    val title: String = "",
    val url: String = "",
)

@Serializable
internal data class VkWallDto(
    val id: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    val text: String? = null,
)

@Serializable
internal data class VkMarketDto(
    val id: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    val price: VkMarketPriceDto? = null,
    val title: String = "",
) {

    @Serializable
    internal data class VkMarketPriceDto(val text: String = "")

}

@Serializable
internal data class VkGraffitiDto(
    val id: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0,
    val url: String = "",
)

// ── Upload DTOs ───────────────────────────────────────────────────────────────

@Serializable
internal data class VkDocUploadServerResponse(
    @SerialName("upload_url") val uploadUrl: String,
)

@Serializable
internal data class VkDocUploadResponse(
    @SerialName("file") val file: String,
)

@Serializable
internal data class VkSavedDocResponse(
    @SerialName("type") val type: String,
    @SerialName("doc") val doc: VkSavedDoc? = null,
    @SerialName("audio_message") val audioMessage: VkSavedAudioMessage? = null,
)

@Serializable
internal data class VkSavedDoc(
    @SerialName("id") val id: Long,
    @SerialName("owner_id") val ownerId: Long,
)

@Serializable
internal data class VkSavedAudioMessage(
    @SerialName("id") val id: Long,
    @SerialName("owner_id") val ownerId: Long,
)

@Serializable
internal data class VkPhotoUploadServerResponse(
    @SerialName("upload_url") val uploadUrl: String,
)

@Serializable
internal data class VkPhotoUploadResponse(
    @SerialName("hash") val hash: String,
    @SerialName("photo") val photo: String,
    @SerialName("server") val server: Int,
)

@Serializable
internal data class VkSavedPhotoResponse(
    @SerialName("id") val id: Long,
    @SerialName("owner_id") val ownerId: Long,
)

// ── Update-specific DTOs ──────────────────────────────────────────────────────

@Serializable
internal data class MessageNewDto(
    @SerialName("client_info") val clientInfo: VkClientInfoDto? = null,
    val message: VkMessageDto,
)

@Serializable
internal data class VkClientInfoDto(
    @SerialName("button_actions") val buttonActions: List<String> = emptyList(),
    val carousel: Boolean = false,
    @SerialName("inline_keyboard") val inlineKeyboard: Boolean = false,
    val keyboard: Boolean = false,
    @SerialName("lang_id") val langId: Int = 0,
)

internal fun VkClientInfoDto.toDomain() = VkClientInfo(buttonActions, carousel, inlineKeyboard, keyboard, langId)

@Serializable
internal data class MessageAllowDto(
    val key: String = "",
    @SerialName("user_id") val userId: Long = 0,
)

@Serializable
internal data class MessageDenyDto(@SerialName("user_id") val userId: Long = 0)

@Serializable
internal data class MessageReadDto(
    @SerialName("from_id") val fromId: Long = 0,
    val incoming: Boolean = false,
    @SerialName("peer_id") val peerId: Long = 0,
    @SerialName("read_message_id") val readMessageId: Int = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
internal data class MessageReactionDto(
    val cmid: Int = 0,
    @SerialName("from_id") val fromId: Long = 0,
    @SerialName("peer_id") val peerId: Long = 0,
    @SerialName("reaction_id") val reactionId: Int = 0,
)

@Serializable
internal data class TypingStateDto(
    @SerialName("from_id") val fromId: Long = 0,
    val state: String = "",
    @SerialName("to_id") val toId: Long = 0,
)

// ── Update parser ─────────────────────────────────────────────────────────────

internal fun VkRawUpdate.toVkUpdate(json: Json): VkUpdate? = when (type) {
    "message_allow" -> json.decodeFromJsonElement<MessageAllowDto>(obj).let {
        VkUpdate.MessageAllow(it.key, it.userId)
    }
    "message_deny" -> VkUpdate.MessageDeny(json.decodeFromJsonElement<MessageDenyDto>(obj).userId)
    "message_edit" -> VkUpdate.MessageEdit(json.decodeFromJsonElement<VkMessageDto>(obj).toDomain())
    "message_new" -> json.decodeFromJsonElement<MessageNewDto>(obj).let {
        VkUpdate.MessageNew(it.clientInfo?.toDomain(), it.message.toDomain())
    }
    "message_reaction_event" -> json.decodeFromJsonElement<MessageReactionDto>(obj).let {
        VkUpdate.MessageReaction(it.cmid, it.fromId, it.peerId, it.reactionId.takeIf { id -> id != 0 })
    }
    "message_read" -> json.decodeFromJsonElement<MessageReadDto>(obj).let {
        VkUpdate.MessageRead(it.fromId, it.incoming, it.peerId, it.readMessageId, it.unreadCount)
    }
    "message_reply" -> VkUpdate.MessageReply(json.decodeFromJsonElement<VkMessageDto>(obj).toDomain())
    "message_typing_state" -> json.decodeFromJsonElement<TypingStateDto>(obj).let {
        VkUpdate.TypingState(it.fromId, it.state, it.toId)
    }
    else -> null
}
