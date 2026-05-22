@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.forum.createForumTopic
import dev.inmo.tgbotapi.extensions.api.chat.modify.pinChatMessage
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ForumTopic
import dev.inmo.tgbotapi.types.MessageThreadId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import dev.inmo.tgbotapi.types.toChatId
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketEvent
import me.soknight.easydesk.supervisor.api.model.Ticket
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.registry.TelegramTopicRegistry
import org.koin.core.annotation.Single

@Single
class TelegramTicketEventHandler(
    private val agentRepository: AgentRepository,
    private val channelIdentityRepository: ChannelIdentityRepository,
    private val config: TelegramSupervisorConfig,
    private val conversationRepository: ConversationRepository,
    private val ticketRepository: TicketRepository,
    private val topicRegistry: TelegramTopicRegistry,
) {

    private val logger = getLogger()

    fun start(scope: CoroutineScope, bot: TelegramBot, eventBus: EventBus) {
        scope.launch {
            eventBus.events
                .filterIsInstance<TicketEvent>()
                .collect { event ->
                    try {
                        when (event) {
                            is TicketEvent.Assigned -> postSystemMessage(
                                bot,
                                event.ticket.identifier,
                                "✅ Assigned to @${resolveAgentName(event.ticket.assignedAgentId)}",
                            )
                            is TicketEvent.Created -> handleCreated(bot, event.ticket)
                            is TicketEvent.Merged -> Unit
                            is TicketEvent.PriorityChanged -> {
                                val agentName = resolveAgentName(event.ticket.assignedAgentId)
                                postSystemMessage(
                                    bot,
                                    event.ticket.identifier,
                                    "🔺 Priority → ${event.ticket.priority.name} (changed by @$agentName)",
                                )
                            }
                            is TicketEvent.Released -> postSystemMessage(
                                bot,
                                event.ticket.identifier,
                                "↩️ Released by @${resolveAgentName(event.previousAgentId)}",
                            )
                            is TicketEvent.StatusChanged -> when (event.ticket.status) {
                                Ticket.Status.CLOSED -> postSystemMessage(
                                    bot,
                                    event.ticket.identifier,
                                    "🔒 Closed by @${resolveAgentName(event.ticket.assignedAgentId)}",
                                )
                                Ticket.Status.RESOLVED -> postSystemMessage(
                                    bot,
                                    event.ticket.identifier,
                                    "✅ Resolved by @${resolveAgentName(event.ticket.assignedAgentId)}",
                                )
                                else -> Unit
                            }
                            is TicketEvent.Tagged -> Unit
                            is TicketEvent.Untagged -> Unit
                        }
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to handle ticket event: $event" }
                    }
                }
        }
    }

    private fun formatTime(instant: Instant): String {
        val ldt = instant.toLocalDateTime(TimeZone.UTC)
        return "%02d:%02d".format(ldt.hour, ldt.minute)
    }

    private suspend fun handleCreated(bot: TelegramBot, ticket: Ticket) {
        val conv = conversationRepository.findById(ticket.conversationId)
        if (conv == null) {
            logger.warn { "Conversation not found for ticket #${ticket.identifier}, " +
                "conversationId=${ticket.conversationId}" }
            return
        }

        val identity = channelIdentityRepository.findById(conv.identityId)
        val clientName = identity?.displayName ?: "Unknown"
        val topicName = "#${ticket.identifier} $clientName"

        val topic = bot.createForumTopic(
            config.supergroupId.toChatId(),
            topicName,
            ForumTopic.CYAN,
        )
        val topicId = topic.messageThreadId.long

        ticketRepository.linkSupervisor(ticket.identifier, TelegramSupervisorBrand, topicId.toString())
        topicRegistry.register(ticket.identifier, topicId)

        val cardText = buildString {
            appendLine("🎫 Ticket #${ticket.identifier} | ${ticket.status.name} | ${ticket.priority.name} priority")
            appendLine("👤 Client: $clientName")
            append("📅 Created: ${formatTime(ticket.createdAt)}")
        }

        val cardMessage = bot.sendMessage(
            chatId = config.supergroupId.toChatId(),
            text = cardText,
            parseMode = HTMLParseMode,
            threadId = MessageThreadId(topicId),
            replyMarkup = InlineKeyboardMarkup(
                keyboard = listOf(
                    listOf(
                        CallbackDataInlineKeyboardButton(
                            text = "▶ Take into work",
                            callbackData = "assign_me:${ticket.identifier}",
                        ),
                        URLInlineKeyboardButton(
                            text = "📱 Open in Mini App (soon)",
                            url = "https://t.me",
                        ),
                    ),
                ),
            ),
        )

        bot.pinChatMessage(
            chatId = config.supergroupId.toChatId(),
            messageId = cardMessage.messageId,
            disableNotification = true,
        )
    }

    private suspend fun postSystemMessage(bot: TelegramBot, ticketId: Long, text: String) {
        val topicId = topicRegistry.getTopicId(ticketId)
            ?: ticketRepository.findSupervisorBinding(ticketId, TelegramSupervisorBrand)?.toLong()

        if (topicId == null) {
            logger.warn { "No topic binding found for ticket #$ticketId, cannot post system message" }
            return
        }

        bot.sendMessage(
            chatId = config.supergroupId.toChatId(),
            text = text,
            threadId = MessageThreadId(topicId),
        )
    }

    private suspend fun resolveAgentName(agentId: Uuid?): String {
        if (agentId == null) return "unknown"
        return agentRepository.findById(agentId)?.displayName ?: "unknown"
    }

}
