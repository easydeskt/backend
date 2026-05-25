@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.extensions.threadIdOrNull
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketMessageEvent
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.registry.TelegramRelayedMessageRegistry
import org.koin.core.annotation.Single
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@Single
class TelegramAgentReplyHandler(
    private val agentRepository: AgentRepository,
    private val conversationRegistry: ConversationRegistry,
    private val eventBus: EventBus,
    private val relayedMessageRegistry: TelegramRelayedMessageRegistry,
    private val ticketMessageRepository: TicketMessageRepository,
    private val ticketRepository: TicketRepository,
) {

    private val logger = getLogger()

    fun start(behaviourContext: BehaviourContext, bot: TelegramBot) {
        behaviourContext.onContentMessage { message ->
            try {
                message.threadIdOrNull ?: return@onContentMessage
                val replyTo = message.replyTo ?: return@onContentMessage
                val relayed = relayedMessageRegistry.getOrNull(replyTo.messageId.long)
                    ?: return@onContentMessage
                val from = (message as? OptionallyFromUserMessage)?.from ?: return@onContentMessage
                val userId = from.id.chatId.long.toString()
                val agent = agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId)
                    ?: return@onContentMessage
                // skip media-only messages for MVP
                val agentText = (message.content as? TextContent)?.text ?: return@onContentMessage
                val conversation = resolveConversation(relayed.conversationId) ?: return@onContentMessage
                conversation.send { plainText = agentText }
                val ticketMessage = ticketMessageRepository.create(
                    ticketId = relayed.ticketId,
                    nativeId = message.messageId.long.toString(),
                    senderKind = ActorKind.AGENT,
                    senderAgentId = agent.identifier,
                    senderIdentityId = null,
                    plainText = agentText,
                    inReplyToNativeId = replyTo.messageId.long.toString(),
                    platformTimestamp = Instant.fromEpochMilliseconds(message.date.unixMillisLong),
                )
                ticketRepository.updateReadMarker(relayed.ticketId, ticketMessage.identifier)
                eventBus.publish(TicketMessageEvent.Recorded(
                    conversationId = relayed.conversationId,
                    message = ticketMessage,
                ))
            } catch (e: Exception) {
                logger.warn(e) { "Failed to handle agent reply: ${e.message}" }
            }
        }
    }

    private suspend fun resolveConversation(conversationId: Long): Conversation? =
        conversationRegistry.getOrNull(conversationId) ?: run {
            logger.warn { "Conversation $conversationId not in live registry (post-restart?), reply dropped" }
            null
        }

}
