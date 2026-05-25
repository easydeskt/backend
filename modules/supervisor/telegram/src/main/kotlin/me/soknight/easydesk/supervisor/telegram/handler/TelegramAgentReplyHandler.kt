@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.extensions.threadIdOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.telegram.TelegramAttachmentParser
import me.soknight.easydesk.channel.telegram.TelegramBrand
import me.soknight.easydesk.channel.telegram.TelegramChannel
import me.soknight.easydesk.channel.telegram.config.TelegramConfig
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketMessageEvent
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.registry.TelegramRelayedMessageRegistry
import org.koin.core.annotation.Single
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@Single
class TelegramAgentReplyHandler(
    private val agentRepository: AgentRepository,
    private val config: TelegramSupervisorConfig,
    private val conversationRegistry: ConversationRegistry,
    private val eventBus: EventBus,
    private val relayedMessageRegistry: TelegramRelayedMessageRegistry,
    private val ticketMessageAttachmentRepository: TicketMessageAttachmentRepository,
    private val ticketMessageRepository: TicketMessageRepository,
    private val ticketRepository: TicketRepository,
) {

    private val logger = getLogger()

    /** Sentinel channel representing the supervisor bot as the attachment source. */
    internal val supervisorChannel by lazy {
        TelegramChannel(
            identifier = "supervisor",
            humanName = "Supervisor",
            config = TelegramConfig(token = config.token),
        )
    }

    fun start(behaviourContext: BehaviourContext, bot: TelegramBot) {
        behaviourContext.onContentMessage { message ->
            try {
                handleAgentMessage(message, bot)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to handle agent reply: ${e.message}" }
            }
        }
    }

    internal suspend fun handleAgentMessage(message: ContentMessage<*>, bot: TelegramBot) {
        message.threadIdOrNull ?: return
        val replyTo = message.replyTo ?: return
        val relayed = relayedMessageRegistry.getOrNull(replyTo.messageId.long) ?: return
        val from = (message as? OptionallyFromUserMessage)?.from ?: return
        val userId = from.id.chatId.long.toString()
        val agent = agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId) ?: return
        val agentText = (message.content as? TextContent)?.text
        val parsedAttachments = TelegramAttachmentParser.parse(message, bot, supervisorChannel)
        if (agentText.isNullOrBlank() && parsedAttachments.isEmpty()) return
        val conversation = resolveConversation(relayed.conversationId) ?: return
        conversation.send(replyToNativeId = replyTo.messageId.long.toString()) {
            plainText = agentText
            if (parsedAttachments.isNotEmpty()) {
                attachments { addAll(parsedAttachments) }
            }
        }
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
        for (attachment in parsedAttachments) {
            runCatching {
                ticketMessageAttachmentRepository.create(
                    messageId = ticketMessage.identifier,
                    kind = attachment.kind,
                    fileName = attachment.fileName,
                    contentType = attachment.contentType,
                    fileSize = attachment.fileSize,
                    channelBrand = TelegramBrand.identifier,
                    attributes = JsonObject(attachment.attributes),
                )
            }.onFailure {
                if (it is CancellationException) throw it
                logger.warn(it) { "Failed to persist attachment metadata" }
            }
        }
        ticketRepository.updateReadMarker(relayed.ticketId, ticketMessage.identifier)
        eventBus.publish(TicketMessageEvent.Recorded(
            conversationId = relayed.conversationId,
            message = ticketMessage,
        ))
    }

    private suspend fun resolveConversation(conversationId: Long): Conversation? =
        conversationRegistry.getOrNull(conversationId) ?: run {
            logger.warn { "Conversation $conversationId not in live registry (post-restart?), reply dropped" }
            null
        }

}
