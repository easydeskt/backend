@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import me.soknight.easydesk.core.KeyedEnum

/**
 * Read-only view of a ticket for supervisor surfaces.
 */
interface Ticket {

    /** Id of the currently assigned agent, or `null` if unassigned. */
    val assignedAgentId: Uuid?

    /** Id of the conversation this ticket belongs to. */
    val conversationId: Long

    /** Timestamp of creation. */
    val createdAt: Instant

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** Id of the ticket this one was merged into; non-null iff [status] is [Status.MERGED]. */
    val mergedIntoTicketId: Long?

    /** Operational urgency level. */
    val priority: Priority

    /** Current lifecycle state. */
    val status: Status

    /** Priority level of the ticket, used for queue ordering. */
    @Serializable(with = Priority.Serializer::class)
    enum class Priority(override val key: String) : KeyedEnum {

        HIGH    ("high"),
        LOW     ("low"),
        MEDIUM  ("medium"),
        ;

        object Serializer : KSerializer<Priority> by KeyedEnum.serializer()

    }

    /** Lifecycle state of the ticket. */
    @Serializable(with = Status.Serializer::class)
    enum class Status(override val key: String) : KeyedEnum {

        CLOSED      ("closed"),
        IN_PROGRESS ("in_progress"),
        MERGED      ("merged"),
        OPEN        ("open"),
        RESOLVED    ("resolved"),
        ;

        object Serializer : KSerializer<Status> by KeyedEnum.serializer()

    }

}
