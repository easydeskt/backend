@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity
import me.soknight.easydesk.supervisor.api.model.TicketMessage as SupervisorTicketMessage

/**
 * A single message within a ticket thread.
 *
 * Messages are immutable records of platform events. [nativeId] is the platform-specific
 * message identifier used for deduplication and reply threading.
 *
 * Exactly one of [senderAgentId] / [senderIdentityId] is non-null according to [senderKind]:
 * - [ActorKind.AGENT] → [senderAgentId] is set
 * - [ActorKind.IDENTITY] → [senderIdentityId] is set
 * - [ActorKind.SYSTEM] → both are `null`
 *
 * Implements [Message] to expose the stored record as a first-class channel message,
 * allowing it to be passed back into the channel:api pipeline (e.g., for replies).
 * Platform-mutating operations ([delete], [edit]) throw [UnsupportedOperationException]
 * because stored records cannot be mutated through the repository layer.
 *
 * @param identifier internal auto-generated identifier
 * @param ticketId id of the owning ticket
 * @param nativeId platform-specific message id (used for deduplication)
 * @param senderKind role of the sender
 * @param senderAgentId id of the sending agent, or `null`
 * @param senderIdentityId id of the sending identity, or `null`
 * @param plainText text body of the message, or `null` for media-only messages
 * @param inReplyToNativeId native id of the message being replied to, or `null`
 * @param platformTimestamp when the message was sent on the platform
 * @param rawAttributes extensible platform-specific metadata stored as [JsonObject]
 * @param createdAt timestamp of local recording
 * @param conversation the live channel:api [Conversation] this message belongs to
 * @param attachments attachments associated with this message
 * @param senderIdentity resolved [ChannelIdentity] for [ActorKind.IDENTITY] senders, or `null`
 */
class TicketMessage(
    override val identifier: Long,
    override val ticketId: Long,
    override val nativeId: String,
    override val senderKind: ActorKind,
    override val senderAgentId: Uuid?,
    override val senderIdentityId: Long?,
    override val plainText: String?,
    override val inReplyToNativeId: String?,
    override val platformTimestamp: Instant,
    val rawAttributes: JsonObject,
    val createdAt: Instant = Clock.System.now(),
    // injected by repository at materialization time:
    override val conversation: Conversation,
    override val attachments: Attachments,
    private val senderIdentity: ChannelIdentity?,
) : SupervisorTicketMessage, Message {

    override val attributes: Attributes get() = rawAttributes

    override val sender: ChannelActor get() = when (senderKind) {
        ActorKind.AGENT    -> AgentActor(senderAgentId!!)
        ActorKind.IDENTITY -> senderIdentity ?: ChannelActor.Unknown
        ActorKind.SYSTEM   -> ChannelActor.System
    }

    override val receiver: ChannelActor get() = when (senderKind) {
        ActorKind.IDENTITY                -> ChannelActor.System
        // for SYSTEM sender the conversation's ChannelIdentity is not denormalized onto the message
        ActorKind.AGENT, ActorKind.SYSTEM -> senderIdentity ?: ChannelActor.Unknown
    }

    override fun copy(block: MessageBuilder.() -> Unit): MessageBuilder =
        throw UnsupportedOperationException("TicketMessage does not expose a MessageBuilder copy")

    override suspend fun delete(): Unit =
        throw UnsupportedOperationException("Stored TicketMessage cannot be deleted from the platform")

    override suspend fun edit(block: MessageBuilder.() -> Unit): Message =
        throw UnsupportedOperationException("Stored TicketMessage cannot be edited from the platform")

    override suspend fun reply(block: MessageBuilder.() -> Unit): Message =
        conversation.send(replyToNativeId = nativeId, block = block)

    private class AgentActor(override val agentId: Uuid) : ChannelActor.Agent

}
