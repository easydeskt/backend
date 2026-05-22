package me.soknight.easydesk.service.channels.data.domain

import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A persistent link between a [Channel] and a [ChannelIdentity].
 *
 * Conversations represent the long-lived context of a client's interactions through a
 * specific channel. A client can have at most one conversation per channel (enforced by
 * `UNIQUE (channel_id, identity_id)`). Tickets are created within a conversation.
 *
 * @param id internal auto-generated identifier
 * @param channelId id of the owning channel
 * @param identityId id of the client identity
 * @param attributes extensible platform-specific metadata
 * @param createdAt timestamp of first interaction
 */
data class Conversation(
    val id: Long,
    val channelId: Long,
    val identityId: Long,
    val attributes: JsonObject,
    val createdAt: Instant = Clock.System.now(),
)
