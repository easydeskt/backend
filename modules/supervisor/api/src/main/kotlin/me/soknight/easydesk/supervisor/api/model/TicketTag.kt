package me.soknight.easydesk.supervisor.api.model

import kotlin.time.Instant

/**
 * Read-only view of a ticket tag for supervisor surfaces.
 */
interface TicketTag {

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** Unique label; max 32 characters. */
    val humanName: String

    /**
     * Optional RGBA color packed as a signed 32-bit integer.
     * Bytes from most-significant to least-significant: R G B A.
     */
    val color: Int?

    /** Timestamp of creation. */
    val createdAt: Instant

}
