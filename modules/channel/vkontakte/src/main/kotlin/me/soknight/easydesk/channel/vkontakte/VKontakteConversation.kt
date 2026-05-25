package me.soknight.easydesk.channel.vkontakte

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.vkontakte.vk.VkBot
import me.soknight.easydesk.channel.vkontakte.vk.model.VkMessage
import me.soknight.easydesk.core.logging.getLogger

class VKontakteConversation(
    override val attributes: Attributes = emptyMap(),
    val bot: VkBot,
    override val channel: VKontakteChannel,
    val peerId: Long,
) : Conversation {

    private val logger = getLogger()

    override suspend fun send(message: Message, replyToNativeId: String?): Message {
        val attachmentStrings = message.attachments
            .filter { it.kind != Attachment.Kind.STICKER }
            .mapNotNull { uploadAttachment(it) }

        val cmid = bot.apiClient.sendMessage(
            peerId = peerId,
            text = message.plainText ?: "",
            attachments = attachmentStrings,
            replyTo = replyToNativeId?.toIntOrNull(),
        )
        val sentVkMessage = VkMessage(
            attachments = emptyList(),
            conversationMessageId = cmid,
            date = System.currentTimeMillis() / 1000,
            fromId = 0,
            fwdMessages = emptyList(),
            geo = null,
            id = 0,
            isOut = true,
            peerId = peerId,
            replyMessage = null,
            text = message.plainText ?: "",
        )
        return VKontakteMessage(
            conversation = this,
            vkMessage = sentVkMessage,
            sender = ChannelActor.System,
            receiver = ChannelActor.Unknown,
        )
    }

    override suspend fun send(replyToNativeId: String?, block: MessageBuilder.() -> Unit): Message {
        val builder = VKontakteMessageBuilder().apply(block)
        val cmid = bot.apiClient.sendMessage(
            peerId = peerId,
            text = builder.plainText ?: "",
            replyTo = replyToNativeId?.toIntOrNull(),
        )
        val sentVkMessage = VkMessage(
            attachments = emptyList(),
            conversationMessageId = cmid,
            date = System.currentTimeMillis() / 1000,
            fromId = 0,
            fwdMessages = emptyList(),
            geo = null,
            id = 0,
            isOut = true,
            peerId = peerId,
            replyMessage = null,
            text = builder.plainText ?: "",
        )
        return VKontakteMessage(
            conversation = this,
            vkMessage = sentVkMessage,
            sender = ChannelActor.System,
            receiver = ChannelActor.Unknown,
            overrideAttributes = builder.builtAttributes,
        )
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private suspend fun uploadAttachment(attachment: Attachment): String? = runCatching {
        when (attachment.kind) {
            Attachment.Kind.PHOTO -> {
                val serverResp = bot.apiClient.getPhotoUploadServer(peerId)
                val bytes = withContext(Dispatchers.IO) { attachment.contentSource.readByteArray() }
                val uploadResp = bot.apiClient.uploadPhotoBytes(serverResp.uploadUrl, bytes, attachment.fileName)
                val saved = bot.apiClient.savePhoto(uploadResp.server, uploadResp.photo, uploadResp.hash).first()
                "photo${saved.ownerId}_${saved.id}"
            }
            else -> {
                val docType = when (attachment.kind) {
                    Attachment.Kind.VOICE -> "audio_message"
                    else -> "doc"
                }
                val serverResp = bot.apiClient.getDocUploadServer(peerId, docType)
                val bytes = withContext(Dispatchers.IO) { attachment.contentSource.readByteArray() }
                val uploadResp = bot.apiClient.uploadDocBytes(serverResp.uploadUrl, bytes, attachment.fileName)
                val saved = bot.apiClient.saveDoc(uploadResp.file)
                when {
                    saved.doc != null -> "doc${saved.doc.ownerId}_${saved.doc.id}"
                    saved.audioMessage != null -> "doc${saved.audioMessage.ownerId}_${saved.audioMessage.id}"
                    else -> null
                }
            }
        }
    }.onFailure { logger.warn(it) { "Failed to upload attachment '${attachment.fileName}' to VK" } }.getOrNull()

}
