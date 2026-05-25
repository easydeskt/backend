package me.soknight.easydesk.channel.telegram.domain

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.media.sendAudio
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.media.sendVideo
import dev.inmo.tgbotapi.extensions.api.send.media.sendVoice
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.media.MediaGroupMemberTelegramMedia
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.media.TelegramMediaVideo
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.telegram.TelegramAttachment
import me.soknight.easydesk.channel.telegram.dsl.TelegramMessageBuilder
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn

/**
 * Telegram-specific [Conversation] that sends messages to a user's private chat.
 *
 * @property bot the [TelegramBot] instance used to dispatch API calls
 * @property userChatId the Telegram chat identifier for the target user
 */
class TelegramConversation(
    override val attributes: Attributes,
    val bot: TelegramBot,
    override val channel: Channel,
    val userChatId: ChatIdentifier,
) : Conversation {

    private val logger = getLogger()

    override suspend fun send(message: Message, replyToNativeId: String?): Message {
        val replyParams = replyToNativeId?.let { ReplyParameters(userChatId, MessageId(it.toLong())) }
        val attachments = message.attachments
        val text = message.plainText

        if (attachments.isEmpty()) {
            val sent = bot.sendMessage(
                chatId = userChatId,
                text = text ?: "",
                replyParameters = replyParams,
            )
            return makeTelegramMessage(sent.messageId, message)
        }

        val singleSticker = attachments.singleOrNull { it.kind == Attachment.Kind.STICKER }
        val mediaAttachments = attachments.filter { it.kind != Attachment.Kind.STICKER }

        if (singleSticker != null) {
            val fileId = singleSticker.attributes["telegram.file_id"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            if (fileId != null) {
                val sent = bot.sendSticker(
                    chatId = userChatId,
                    sticker = FileId(fileId),
                    replyParameters = replyParams,
                )
                if (mediaAttachments.isEmpty()) return makeTelegramMessage(sent.messageId, message)
            } else {
                logger.warn { "Sticker has no telegram.file_id — skipping send" }
                if (mediaAttachments.isEmpty()) return makeTelegramMessage(MessageId(0L), message)
            }
            // fall through to send media attachments too
        }

        if (mediaAttachments.size == 1) {
            return sendSingle(mediaAttachments.single(), text, replyParams, message)
        }

        val groups = mediaAttachments.chunked(10)
        var firstSent: Message? = null
        for ((index, group) in groups.withIndex()) {
            val caption = if (index == 0) text else null
            val sent = sendMediaGroupChunk(group, caption, replyParams, message)
            if (firstSent == null) firstSent = sent
        }
        return firstSent ?: makeTelegramMessage(MessageId(0L), message)
    }

    override suspend fun send(replyToNativeId: String?, block: MessageBuilder.() -> Unit): Message {
        val builder = TelegramMessageBuilder().apply(block)
        val built = buildMessage(builder)
        return send(built, replyToNativeId)
    }

    // -------------- PRIVATE IMPLEMENTATION ---------------------------------------------------------------------------

    private fun buildMessage(builder: TelegramMessageBuilder): Message {
        val builtText = builder.plainText
        val builtAttachments = builder.builtAttachments
        val builtAttributes = builder.builtAttributes
        return object : Message {
            override val conversation: Conversation = this@TelegramConversation
            override val nativeId: String = ""
            override val sender: ChannelActor = ChannelActor.System
            override val receiver: ChannelActor = ChannelActor.Unknown
            override val attachments = builtAttachments
            override val plainText: String? = builtText
            override val attributes: Attributes = builtAttributes
            override fun copy(block: MessageBuilder.() -> Unit): MessageBuilder =
                TelegramMessageBuilder().apply {
                    plainText = builtText
                    attachments { addAll(builtAttachments) }
                    attributes { putAll(builtAttributes) }
                }.apply(block)
            override suspend fun delete(): Unit = throw UnsupportedOperationException("Cannot delete unsent message")
            override suspend fun edit(block: MessageBuilder.() -> Unit): Message =
                throw UnsupportedOperationException("Cannot edit unsent message")
            override suspend fun reply(block: MessageBuilder.() -> Unit): Message =
                this@TelegramConversation.send(replyToNativeId = nativeId, block = block)
        }
    }

    private fun attachmentInputFile(attachment: Attachment): InputFile {
        // prefer bytes when available (file_ids are bot-scoped; bytes allow cross-bot forwarding)
        val bytes = (attachment as? TelegramAttachment)?.bytes
        if (bytes != null) return bytes.asMultipartFile(attachment.fileName)
        val fileId = attachment.attributes["telegram.file_id"]?.let { (it as? JsonPrimitive)?.contentOrNull }
        if (fileId != null) return FileId(fileId)
        error("Attachment '${attachment.fileName}' has neither cached bytes nor a telegram.file_id")
    }

    private suspend fun sendSingle(
        attachment: Attachment,
        text: String?,
        replyParams: ReplyParameters?,
        sourceMessage: Message,
    ): Message {
        val inputFile = attachmentInputFile(attachment)
        val sentId = when (attachment.kind) {
            Attachment.Kind.AUDIO -> bot.sendAudio(
                chatId = userChatId,
                audio = inputFile,
                text = text,
                replyParameters = replyParams,
            ).messageId
            Attachment.Kind.DOCUMENT -> bot.sendDocument(
                chatId = userChatId,
                document = inputFile,
                text = text,
                replyParameters = replyParams,
            ).messageId
            Attachment.Kind.PHOTO -> bot.sendPhoto(
                chatId = userChatId,
                fileId = inputFile,
                text = text,
                replyParameters = replyParams,
            ).messageId
            Attachment.Kind.STICKER -> error("Sticker must be handled before sendSingle()")
            Attachment.Kind.VIDEO -> bot.sendVideo(
                chatId = userChatId,
                video = inputFile,
                text = text,
                replyParameters = replyParams,
            ).messageId
            Attachment.Kind.VOICE -> bot.sendVoice(
                chatId = userChatId,
                voice = inputFile,
                text = text,
                replyParameters = replyParams,
            ).messageId
        }
        return makeTelegramMessage(sentId, sourceMessage)
    }

    @OptIn(RiskFeature::class)
    private suspend fun sendMediaGroupChunk(
        attachments: List<Attachment>,
        text: String?,
        replyParams: ReplyParameters?,
        sourceMessage: Message,
    ): Message {
        val media: List<MediaGroupMemberTelegramMedia> = attachments.mapIndexed { index, attachment ->
            val caption = if (index == 0) text else null
            val inputFile = attachmentInputFile(attachment)
            when (attachment.kind) {
                Attachment.Kind.PHOTO -> TelegramMediaPhoto(file = inputFile, text = caption)
                Attachment.Kind.VIDEO -> TelegramMediaVideo(file = inputFile, text = caption)
                else -> TelegramMediaDocument(file = inputFile, text = caption)
            }
        }
        val sent = bot.sendMediaGroup(
            chatId = userChatId,
            media = media,
            replyParameters = replyParams,
        )
        return makeTelegramMessage(sent.messageId, sourceMessage)
    }

    private fun makeTelegramMessage(messageId: MessageId, sourceMessage: Message) = TelegramMessage(
        conversation = this,
        messageId = messageId,
        sender = ChannelActor.System,
        receiver = ChannelActor.Unknown,
        plainText = sourceMessage.plainText,
        attachments = sourceMessage.attachments,
        attributes = sourceMessage.attributes,
    )

}
