@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram.command

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.types.message.textsources.TextMentionTextSource
import kotlin.uuid.ExperimentalUuidApi
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketEvent
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import org.koin.core.annotation.Single

@Single
class AssignCommand(
    private val agentRepository: AgentRepository,
    private val eventBus: EventBus,
    private val ticketRepository: TicketRepository,
) {

    suspend fun execute(message: TextMessage, bot: TelegramBot, agent: Agent, ticketId: Long) {
        val args = message.content.text.substringAfter("/assign").trim()

        val targetAgentId = when {
            args == "me" -> agent.identifier
            args.startsWith("@") -> {
                val mention = message.content.textSources
                    .filterIsInstance<TextMentionTextSource>()
                    .firstOrNull()
                if (mention != null) {
                    val userId = mention.user.id.chatId.long
                    agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId.toString())
                        ?.identifier
                } else {
                    agentRepository.findAll()
                        .firstOrNull { it.displayName.equals(args.removePrefix("@"), ignoreCase = true) }
                        ?.identifier
                }
            }
            else -> agentRepository.findAll()
                .firstOrNull { it.displayName.equals(args, ignoreCase = true) }
                ?.identifier
        }

        if (targetAgentId == null) {
            bot.reply(message, "⚠️ Agent not found.")
            return
        }

        val currentTicket = ticketRepository.findById(ticketId)
        if (currentTicket == null) {
            bot.reply(message, "⚠️ Ticket not found.")
            return
        }

        val updatedTicket = ticketRepository.assign(ticketId, targetAgentId)
        if (updatedTicket == null) {
            bot.reply(message, "⚠️ Ticket not found.")
            return
        }

        eventBus.publish(
            TicketEvent.Assigned(
                previousAgentId = currentTicket.assignedAgentId,
                ticket = updatedTicket,
            ),
        )
    }

}
