package me.soknight.easydesk.supervisor.api.event

import me.soknight.easydesk.core.event.Event
import me.soknight.easydesk.supervisor.api.model.AuditEvent

/**
 * Published when a new [AuditEvent] is persisted.
 *
 * Consumed by supervisor surfaces to append entries to the pinned audit topic in the supergroup.
 *
 * @param event the newly recorded audit event
 * @see AuditEvent
 */
data class AuditEventRecorded(val event: AuditEvent) : Event
