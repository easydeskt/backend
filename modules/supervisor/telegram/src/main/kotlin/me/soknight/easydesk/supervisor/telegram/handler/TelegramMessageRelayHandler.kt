package me.soknight.easydesk.supervisor.telegram.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.MessageThreadId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketMessageEvent
import me.soknight.easydesk.supervisor.api.model.ActorKind
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
    private val ticketRepository: TicketRepository,
    private val topicRegistry: TelegramTopicRegistry,
) {

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
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to relay client message: $event" }
                    }
                }
        }
    }

    private suspend fun relayClientMessage(bot: TelegramBot, event: TicketMessageEvent.Recorded) {
        val message = event.message
        val topicId = topicRegistry.getTopicId(message.ticketId)
            ?: ticketRepository.findSupervisorBinding(message.ticketId, TelegramSupervisorBrand)?.toLong()

        if (topicId == null) {
            logger.warn { "No topic binding found for ticket #${message.ticketId}, cannot relay client message" }
            return
        }

        val displayName = channelIdentityRepository.findById(message.senderIdentityId!!)?.displayName ?: "Client"
        val text = "📩 [$displayName]\n${message.plainText ?: "(media)"}"

        val sentMessage = bot.sendMessage(
            chatId = config.supergroupId.toChatId(),
            text = text,
            threadId = MessageThreadId(topicId),
            replyMarkup = InlineKeyboardMarkup(
                keyboard = listOf(listOf(
                    CallbackDataInlineKeyboardButton(
                        text = "✓ Прочитано",
                        callbackData = "mark_read:${message.ticketId}:${message.identifier}",
                    ),
                )),
            ),
        )
        relayedMessageRegistry.register(
            supervisorMessageId = sentMessage.messageId.long,
            conversationId = event.conversationId,
            ticketId = message.ticketId,
        )
    }

}
