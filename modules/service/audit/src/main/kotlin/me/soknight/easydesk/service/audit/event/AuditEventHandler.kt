@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.audit.event

import kotlin.uuid.ExperimentalUuidApi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.audit.data.repository.AuditEventRepository
import me.soknight.easydesk.supervisor.api.event.TicketEvent
import org.koin.core.annotation.Single

/**
 * Records audit events for system-triggered ticket lifecycle changes.
 *
 * Agent-triggered changes are recorded directly in the API route handlers.
 * This handler covers events that originate outside the API layer (e.g., inbound messages).
 *
 * Must be started exactly once via [start]; calling it again creates a duplicate subscriber.
 */
@Single
class AuditEventHandler(
    private val auditEventRepository: AuditEventRepository,
    private val eventBus: EventBus,
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            eventBus.events
                .filterIsInstance<TicketEvent.Created>()
                .collect { event ->
                    auditEventRepository.create(
                        eventType = "ticket.created",
                        ticketId = event.ticket.identifier,
                    )
                }
        }
    }

}
