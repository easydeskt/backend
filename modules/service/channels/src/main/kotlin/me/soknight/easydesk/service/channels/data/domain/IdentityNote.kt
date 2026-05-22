@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.channels.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.soknight.easydesk.supervisor.api.model.IdentityNote as SupervisorIdentityNote

/**
 * A free-text note attached to a [ChannelIdentity] by an agent.
 *
 * Notes serve as an internal knowledge base about a client — context visible only
 * to agents and not exposed to the client.
 *
 * @param identifier internal auto-generated identifier
 * @param identityId id of the identity the note is attached to
 * @param text note body
 * @param authorAgentId id of the agent who wrote the note
 * @param createdAt timestamp of creation
 * @param updatedAt timestamp of the last edit
 */
data class IdentityNote(
    override val identifier: Long,
    override val identityId: Long,
    override val text: String,
    override val authorAgentId: Uuid,
    override val createdAt: Instant = Clock.System.now(),
    override val updatedAt: Instant = createdAt,
) : SupervisorIdentityNote
