@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram.command

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlin.uuid.ExperimentalUuidApi
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketEvent
import me.soknight.easydesk.supervisor.api.model.Ticket as SupervisorTicket
import org.koin.core.annotation.Single

@Single
class PriorityCommand(
    private val eventBus: EventBus,
    private val ticketRepository: TicketRepository,
) {

    suspend fun execute(message: TextMessage, bot: TelegramBot, agent: Agent, ticketId: Long) {
        val args = message.content.text.substringAfter("/priority").trim()

        val ticket = when (args) {
            "l", "m", "h" -> null  // no pre-load needed for absolute values
            "+", "-" -> {
                val current = ticketRepository.findById(ticketId)
                if (current == null) {
                    bot.reply(message, "⚠️ Ticket not found.")
                    return
                }
                current
            }
            else -> {
                bot.reply(message, "⚠️ Usage: /priority l|m|h|+|-")
                return
            }
        }

        val newPriority: SupervisorTicket.Priority = when (args) {
            "l" -> SupervisorTicket.Priority.LOW
            "m" -> SupervisorTicket.Priority.MEDIUM
            "h" -> SupervisorTicket.Priority.HIGH
            "+" -> {
                val incremented = increment(ticket!!.priority)
                if (incremented == null) {
                    bot.reply(message, "⚠️ Priority is already at its limit.")
                    return
                }
                incremented
            }
            "-" -> {
                val decremented = decrement(ticket!!.priority)
                if (decremented == null) {
                    bot.reply(message, "⚠️ Priority is already at its limit.")
                    return
                }
                decremented
            }
            else -> return
        }

        val currentTicket = ticket ?: ticketRepository.findById(ticketId)
        if (currentTicket == null) {
            bot.reply(message, "⚠️ Ticket not found.")
            return
        }

        val updatedTicket = ticketRepository.updatePriority(ticketId, newPriority)
        if (updatedTicket == null) {
            bot.reply(message, "⚠️ Ticket not found.")
            return
        }

        eventBus.publish(TicketEvent.PriorityChanged(previous = currentTicket.priority, ticket = updatedTicket))
    }

    private fun decrement(p: SupervisorTicket.Priority) = when (p) {
        SupervisorTicket.Priority.HIGH   -> SupervisorTicket.Priority.MEDIUM
        SupervisorTicket.Priority.MEDIUM -> SupervisorTicket.Priority.LOW
        SupervisorTicket.Priority.LOW    -> null
    }

    private fun increment(p: SupervisorTicket.Priority) = when (p) {
        SupervisorTicket.Priority.LOW    -> SupervisorTicket.Priority.MEDIUM
        SupervisorTicket.Priority.MEDIUM -> SupervisorTicket.Priority.HIGH
        SupervisorTicket.Priority.HIGH   -> null
    }

}
