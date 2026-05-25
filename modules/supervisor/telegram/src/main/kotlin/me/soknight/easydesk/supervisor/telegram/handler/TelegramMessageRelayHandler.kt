package me.soknight.easydesk.supervisor.telegram.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.media.sendAudio
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.media.sendVideo
import dev.inmo.tgbotapi.extensions.api.send.media.sendVoice
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageThreadId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.toChatId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageAttachment
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketMessageEvent
import me.soknight.easydesk.supervisor.api.model.ActorKind
import me.soknight.easydesk.supervisor.api.model.TicketMessage
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.registry.TelegramRelayedMessageRegistry
import me.soknight.easydesk.supervisor.telegram.registry.TelegramTopicRegistry
import org.koin.core.annotation.Single

@Single
class TelegramMessageRelayHandler(
    private val channelIdentityRepository: ChannelIdentityRepository,
    private val config: TelegramSupervisorConfig,
    private val relayedMessageRegistry: TelegramRelayedMessageRegistry,
    private val ticketMessageAttachmentRepository: TicketMessageAttachmentRepository,
    private val ticketRepository: TicketRepository,
    private val topicRegistry: TelegramTopicRegistry,
) {

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    }

    private val logger = getLogger()

    fun start(scope: CoroutineScope, bot: TelegramBot, eventBus: EventBus) {

        scope.launch {
            eventBus.events
                .filterIsInstance<TicketMessageEvent.Recorded>()
                .collect { event ->
                    try {
                        if (event.message.senderKind == ActorKind.IDENTITY) {
                            relayClientMessage(bot, event)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to relay client message: $event" }
                    }
                }
        }
    }

    fun stop() {
        httpClient.close()
    }

    private suspend fun relayClientMessage(bot: TelegramBot, event: TicketMessageEvent.Recorded) {
        val message = event.message
        val topicId = topicRegistry.getTopicId(message.ticketId)
            ?: ticketRepository.findSupervisorBinding(message.ticketId, TelegramSupervisorBrand)?.toLong()

        if (topicId == null) {
            logger.warn { "No topic binding for ticket #${message.ticketId}, cannot relay client message" }
            return
        }

        val displayName = channelIdentityRepository.findById(message.senderIdentityId!!)?.displayName ?: "Client"
        val attachments = ticketMessageAttachmentRepository.findByMessage(message.identifier)
        val chatId = config.supergroupId.toChatId()
        val threadId = MessageThreadId(topicId)
        val readMarkupButton = InlineKeyboardMarkup(listOf(listOf(
            CallbackDataInlineKeyboardButton(
                text = "✓ Прочитано",
                callbackData = "mark_read:${message.ticketId}:${message.identifier}",
            ),
        )))

        if (attachments.isEmpty()) {
            val text = "📩 [$displayName]\n${message.plainText ?: "(сообщение без текста)"}"
            val sent = bot.sendMessage(chatId, text, threadId = threadId, replyMarkup = readMarkupButton)
            relayedMessageRegistry.register(sent.messageId.long, event.conversationId, message.ticketId)
            return
        }

        if (!message.plainText.isNullOrBlank()) {
            bot.sendMessage(chatId, "📩 [$displayName]\n${message.plainText}", threadId = threadId)
        }

        var firstSupervisorMessageId: Long? = null
        for (att in attachments) {
            val sentMsg = relaySingleAttachment(bot, chatId, threadId, att, displayName, message)
                ?: continue
            if (firstSupervisorMessageId == null) {
                firstSupervisorMessageId = sentMsg.messageId.long
            }
        }

        firstSupervisorMessageId?.let { msgId ->
            relayedMessageRegistry.register(msgId, event.conversationId, message.ticketId)
            bot.sendMessage(chatId, "✓", threadId = threadId, replyMarkup = readMarkupButton)
        }
    }

    internal suspend fun relaySingleAttachment(
        bot: TelegramBot,
        chatId: ChatIdentifier,
        threadId: MessageThreadId,
        att: TicketMessageAttachment,
        displayName: String,
        message: TicketMessage,
    ): ContentMessage<*>? {
        val caption = if (message.plainText.isNullOrBlank()) "📩 [$displayName]" else null
        return runCatching {
            when (att.kind) {
                Attachment.Kind.AUDIO -> {
                    val fileId = att.telegramFileId
                    if (fileId != null) {
                        bot.sendAudio(chatId, FileId(fileId), text = caption, threadId = threadId)
                    } else {
                        val bytes = downloadUrl(att) ?: return null
                        bot.sendAudio(chatId, bytes.asMultipartFile(att.fileName), text = caption, threadId = threadId)
                    }
                }
                Attachment.Kind.DOCUMENT -> {
                    val playerUrl = att.vkPlayerUrl
                    if (playerUrl != null) {
                        bot.sendMessage(chatId, "📎 ${att.fileName} — $playerUrl", threadId = threadId)
                    } else {
                        val fileId = att.telegramFileId
                        if (fileId != null) {
                            bot.sendDocument(chatId, FileId(fileId), text = caption, threadId = threadId)
                        } else {
                            val bytes = downloadUrl(att) ?: return null
                            bot.sendDocument(chatId, bytes.asMultipartFile(att.fileName), text = caption, threadId = threadId)
                        }
                    }
                }
                Attachment.Kind.PHOTO -> {
                    val fileId = att.telegramFileId
                    if (fileId != null) {
                        bot.sendPhoto(chatId, FileId(fileId), text = caption, threadId = threadId)
                    } else {
                        val bytes = downloadUrl(att) ?: return null
                        bot.sendPhoto(chatId, bytes.asMultipartFile(att.fileName), text = caption, threadId = threadId)
                    }
                }
                Attachment.Kind.STICKER -> {
                    val fileId = att.telegramFileId ?: return null
                    bot.sendSticker(chatId, FileId(fileId), threadId = threadId)
                }
                Attachment.Kind.VIDEO -> {
                    val fileId = att.telegramFileId
                    if (fileId != null) {
                        bot.sendVideo(chatId, FileId(fileId), text = caption, threadId = threadId)
                    } else {
                        val bytes = downloadUrl(att) ?: return null
                        bot.sendVideo(chatId, bytes.asMultipartFile(att.fileName), text = caption, threadId = threadId)
                    }
                }
                Attachment.Kind.VOICE -> {
                    val fileId = att.telegramFileId
                    if (fileId != null) {
                        bot.sendVoice(chatId, FileId(fileId), threadId = threadId)
                    } else {
                        val bytes = downloadUrl(att) ?: return null
                        bot.sendVoice(chatId, bytes.asMultipartFile(att.fileName), threadId = threadId)
                    }
                }
            }
        }.onFailure {
            if (it is CancellationException) throw it
            logger.warn(it) { "Failed to relay attachment '${att.fileName}' to topic" }
        }.getOrNull()
    }

    private val TicketMessageAttachment.telegramFileId: String?
        get() = (attributes["telegram.file_id"] as? JsonPrimitive)?.contentOrNull

    private val TicketMessageAttachment.vkPlayerUrl: String?
        get() = (attributes["vk.player_url"] as? JsonPrimitive)?.contentOrNull

    private suspend fun downloadUrl(att: TicketMessageAttachment): ByteArray? {
        // TODO: vk.url and email.url are not populated by their respective mappers — cross-channel relay of VK/email attachments is silently dropped until those mappers are updated
        val url = (att.attributes["vk.url"] as? JsonPrimitive)?.contentOrNull
            ?: (att.attributes["email.url"] as? JsonPrimitive)?.contentOrNull
            ?: return null
        return runCatching { httpClient.get(url).body<ByteArray>() }
            .onFailure {
                if (it is CancellationException) throw it
                logger.warn(it) { "Failed to download attachment from $url" }
            }
            .getOrNull()
    }

}
