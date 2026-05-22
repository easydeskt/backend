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
class ResolveCommand(
    private val eventBus: EventBus,
    private val ticketRepository: TicketRepository,
) {

    suspend fun execute(message: TextMessage, bot: TelegramBot, agent: Agent, ticketId: Long) {
        val ticket = ticketRepository.findById(ticketId)
        if (ticket == null) {
            bot.reply(message, "⚠️ Ticket not found.")
            return
        }

        val updatedTicket = ticketRepository.updateStatus(ticketId, SupervisorTicket.Status.RESOLVED)
        if (updatedTicket == null) {
            bot.reply(message, "⚠️ Ticket not found.")
            return
        }

        eventBus.publish(TicketEvent.StatusChanged(previous = ticket.status, ticket = updatedTicket))
    }

}
