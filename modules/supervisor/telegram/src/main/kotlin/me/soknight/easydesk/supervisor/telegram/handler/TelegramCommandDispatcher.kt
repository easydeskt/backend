@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.extensions.threadIdOrNull
import kotlin.uuid.ExperimentalUuidApi
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketEvent
import me.soknight.easydesk.supervisor.api.model.Agent as SupervisorAgent
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.command.AddAgentCommand
import me.soknight.easydesk.supervisor.telegram.command.AssignCommand
import me.soknight.easydesk.supervisor.telegram.command.CloseCommand
import me.soknight.easydesk.supervisor.telegram.command.FreeCommand
import me.soknight.easydesk.supervisor.telegram.command.PriorityCommand
import me.soknight.easydesk.supervisor.telegram.command.ResolveCommand
import me.soknight.easydesk.supervisor.telegram.registry.TelegramTopicRegistry
import org.koin.core.annotation.Single

@Single
class TelegramCommandDispatcher(
    private val addAgentCommand: AddAgentCommand,
    private val agentRepository: AgentRepository,
    private val assignCommand: AssignCommand,
    private val closeCommand: CloseCommand,
    private val eventBus: EventBus,
    private val freeCommand: FreeCommand,
    private val priorityCommand: PriorityCommand,
    private val resolveCommand: ResolveCommand,
    private val ticketRepository: TicketRepository,
    private val topicRegistry: TelegramTopicRegistry,
) {

    suspend fun register(behaviourContext: BehaviourContext, bot: TelegramBot) {
        behaviourContext.onCommand("addagent") { message ->
            val agent = requireAdminAgent(message, bot) ?: return@onCommand
            addAgentCommand.execute(message, bot, agent)
        }
        behaviourContext.onCommand("assign") { message ->
            val agent = requireAgent(message, bot) ?: return@onCommand
            val ticketId = requireTopic(message, bot) ?: return@onCommand
            assignCommand.execute(message, bot, agent, ticketId)
        }
        behaviourContext.onCommand("close") { message ->
            val agent = requireAgent(message, bot) ?: return@onCommand
            val ticketId = requireTopic(message, bot) ?: return@onCommand
            closeCommand.execute(message, bot, agent, ticketId)
        }
        behaviourContext.onCommand("free") { message ->
            val agent = requireAgent(message, bot) ?: return@onCommand
            val ticketId = requireTopic(message, bot) ?: return@onCommand
            freeCommand.execute(message, bot, agent, ticketId)
        }
        behaviourContext.onCommand("priority") { message ->
            val agent = requireAgent(message, bot) ?: return@onCommand
            val ticketId = requireTopic(message, bot) ?: return@onCommand
            priorityCommand.execute(message, bot, agent, ticketId)
        }
        behaviourContext.onCommand("resolve") { message ->
            val agent = requireAgent(message, bot) ?: return@onCommand
            val ticketId = requireTopic(message, bot) ?: return@onCommand
            resolveCommand.execute(message, bot, agent, ticketId)
        }
        behaviourContext.onDataCallbackQuery { callbackQuery ->
            val callbackData = callbackQuery.data
            when {
                callbackData.startsWith("assign_me:") -> {
                    val ticketId = callbackData.removePrefix("assign_me:").toLongOrNull()
                    if (ticketId == null) {
                        bot.answerCallbackQuery(callbackQuery.id, text = "⚠️ Invalid ticket reference.")
                        return@onDataCallbackQuery
                    }

                    val userId = callbackQuery.user.id.chatId.long
                    val agent = agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId.toString())
                    if (agent == null) {
                        bot.answerCallbackQuery(callbackQuery.id, text = "⚠️ You are not registered as an agent.")
                        return@onDataCallbackQuery
                    }

                    val currentTicket = ticketRepository.findById(ticketId)
                    if (currentTicket == null) {
                        bot.answerCallbackQuery(callbackQuery.id, text = "⚠️ Ticket not found.")
                        return@onDataCallbackQuery
                    }

                    val updatedTicket = ticketRepository.assign(ticketId, agent.identifier)
                    if (updatedTicket == null) {
                        bot.answerCallbackQuery(callbackQuery.id, text = "⚠️ Ticket not found.")
                        return@onDataCallbackQuery
                    }

                    eventBus.publish(
                        TicketEvent.Assigned(
                            previousAgentId = currentTicket.assignedAgentId,
                            ticket = updatedTicket,
                        ),
                    )

                    bot.answerCallbackQuery(callbackQuery.id)
                }
                callbackData.startsWith("mark_read:") -> {
                    val parts = callbackData.removePrefix("mark_read:").split(":")
                    val ticketId = parts.getOrNull(0)?.toLongOrNull()
                    val messageId = parts.getOrNull(1)?.toLongOrNull()
                    if (ticketId == null || messageId == null) {
                        bot.answerCallbackQuery(callbackQuery.id, text = "⚠️ Invalid read marker reference.")
                        return@onDataCallbackQuery
                    }

                    val userId = callbackQuery.user.id.chatId.long
                    val agent = agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId.toString())
                    if (agent == null) {
                        bot.answerCallbackQuery(callbackQuery.id, text = "⚠️ You are not registered as an agent.")
                        return@onDataCallbackQuery
                    }

                    val ticket = ticketRepository.findById(ticketId)
                    if (ticket == null) {
                        bot.answerCallbackQuery(callbackQuery.id, text = "⚠️ Ticket not found.")
                        return@onDataCallbackQuery
                    }

                    val currentMarker = ticket.readUpToMessageId
                    if (currentMarker == null || messageId > currentMarker) {
                        ticketRepository.updateReadMarker(ticketId, messageId)
                    }

                    bot.answerCallbackQuery(callbackQuery.id, text = "✓ Marked as read.")
                }
            }
        }
    }

    private suspend fun requireAdminAgent(message: TextMessage, bot: TelegramBot): Agent? {
        val agent = requireAgent(message, bot) ?: return null
        if (agent.role != SupervisorAgent.Role.ADMIN) {
            bot.reply(message, "⚠️ This command requires ADMIN privileges.")
            return null
        }
        return agent
    }

    private suspend fun requireAgent(message: TextMessage, bot: TelegramBot): Agent? {
        val agent = resolveAgent(message)
        if (agent == null) {
            bot.reply(message, "⚠️ You are not registered as an agent.")
            return null
        }
        return agent
    }

    private suspend fun requireTopic(message: TextMessage, bot: TelegramBot): Long? {
        if (message.threadIdOrNull == null) {
            bot.reply(message, "⚠️ This command only works inside a ticket topic.")
            return null
        }
        val ticketId = resolveTicket(message)
        if (ticketId == null) {
            bot.reply(message, "⚠️ Could not resolve ticket for this topic.")
            return null
        }
        return ticketId
    }

    private suspend fun resolveAgent(message: TextMessage): Agent? {
        val userId = (message as? OptionallyFromUserMessage)?.from?.id?.chatId?.long ?: return null
        return agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId.toString())
    }

    private suspend fun resolveTicket(message: TextMessage): Long? {
        val topicId = message.threadIdOrNull?.long ?: return null
        topicRegistry.getTicketId(topicId)?.let { return it }
        val ticketId = ticketRepository.findTicketBySupervisorBinding(
            TelegramSupervisorBrand,
            topicId.toString(),
        ) ?: return null
        topicRegistry.register(ticketId, topicId)
        return ticketId
    }

}
