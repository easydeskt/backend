@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Read-only view of an identity note (agent-written customer annotation) for supervisor surfaces.
 */
interface IdentityNote {

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** Id of the agent who wrote the note. */
    val authorAgentId: Uuid

    /** Id of the identity this note is attached to. */
    val identityId: Long

    /** Note body; visible only to agents, not exposed to the client. */
    val text: String

    /** Timestamp of creation. */
    val createdAt: Instant

    /** Timestamp of the last edit. */
    val updatedAt: Instant

}
