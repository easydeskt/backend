@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.event

import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.event.MessageEvent
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.event.TicketEvent
import me.soknight.easydesk.supervisor.api.event.TicketMessageEvent
import org.koin.core.annotation.Single

private val logger = getLogger("MessageEventHandler")

/**
 * Subscribes to [EventBus] and persists every [MessageEvent.Received] from a real
 * [ChannelActor.Identity] sender into the ticket pipeline:
 *
 *  1. Resolve the service-layer channel by `channelBrand.identifier`.
 *  2. `findOrCreate` the [ChannelIdentity][me.soknight.easydesk.service.channels.data.domain.ChannelIdentity] row.
 *  3. `findOrCreate` the [Conversation][me.soknight.easydesk.service.channels.data.domain.Conversation] row.
 *  4. Register the live [event.message.conversation][me.soknight.easydesk.channel.api.model.Conversation] in [ConversationRegistry].
 *  5. Find or create an OPEN/IN_PROGRESS [Ticket][me.soknight.easydesk.service.tickets.data.domain.Ticket].
 *  6. Deduplicate by `(ticketId, nativeId)` and persist a [TicketMessage][me.soknight.easydesk.service.tickets.data.domain.TicketMessage].
 *  7. Persist each inbound [Attachment]'s metadata to `ticket_message_attachments`.
 */
@Single
class MessageEventHandler(
    private val attachmentStorageService: AttachmentStorageService,
    private val channelIdentityRepository: ChannelIdentityRepository,
    private val channelRepository: ChannelRepository,
    private val conversationRegistry: ConversationRegistry,
    private val conversationRepository: ConversationRepository,
    private val eventBus: EventBus,
    private val ticketMessageAttachmentRepository: TicketMessageAttachmentRepository,
    private val ticketMessageRepository: TicketMessageRepository,
    private val ticketRepository: TicketRepository,
) {

    /**
     * Subscribes to the [EventBus] within [scope]. Must be called exactly once;
     * calling it again creates a duplicate subscriber.
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            eventBus.events
                .filterIsInstance<MessageEvent.Received>()
                .collect { event ->
                    try {
                        handle(event)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logger.error(
                            "Failed to handle MessageEvent.Received (nativeId={}), event dropped",
                            event.message.nativeId, e,
                        )
                    }
                }
        }
    }

    // TODO: wrap the full pipeline in a single transaction once use-cases land
    private suspend fun handle(event: MessageEvent.Received) {
        val sender = event.message.sender as? ChannelActor.Identity ?: return
        val brand = sender.channelBrand
        val channel = channelRepository.findByBrand(brand.identifier, enabledOnly = true)
            .firstOrNull() ?: run {
                logger.warn("Dropping inbound message: no enabled service channel for brand '{}'", brand.identifier)
                return
            }

        val identity = channelIdentityRepository.findOrCreate(
            channelBrand = brand,
            nativeId = sender.nativeId,
            displayName = sender.humanName,
        )

        val conv = conversationRepository.findOrCreate(channel.id, identity.identifier)
        conversationRegistry.register(conv.id, event.message.conversation)

        val ticket = ticketRepository.findOpenByConversation(conv.id)
            ?: ticketRepository.create(conv.id).also { eventBus.publish(TicketEvent.Created(it)) }

        val nativeId = event.message.nativeId
        if (ticketMessageRepository.findByNativeId(ticket.identifier, nativeId) != null) return

        val message = ticketMessageRepository.create(
            ticketId = ticket.identifier,
            nativeId = nativeId,
            senderKind = ActorKind.IDENTITY,
            senderAgentId = null,
            senderIdentityId = identity.identifier,
            plainText = event.message.plainText,
            inReplyToNativeId = null,
            platformTimestamp = event.timestamp,
            attributes = JsonObject(event.message.attributes),
        )

        event.message.attachments.forEach { attachment ->
            persistAttachmentMetadata(attachment, message.identifier, brand.identifier)
        }

        eventBus.publish(TicketMessageEvent.Recorded(conversationId = conv.id, message = message))
    }

    private suspend fun maybeStoreLocally(attachment: Attachment): Map<String, JsonElement> {
        if (attachment.attributes.containsKey("telegram.file_id") || attachment.attributes.containsKey("vk.url")) {
            return emptyMap()
        }
        val storagePath = runCatching {
            // contentSource may be a blocking IMAP stream for email attachments;
            // store() writes to the local filesystem — both warrant Dispatchers.IO
            withContext(Dispatchers.IO) {
                val bytes = attachment.contentSource.readByteArray()
                if (bytes.isEmpty()) return@withContext null
                attachmentStorageService.store(Buffer().also { it.write(bytes) }, attachment.fileName, attachment.kind)
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
            logger.warn("Could not store bytes locally for attachment '${attachment.fileName}': ${e.message}")
        }.getOrNull() ?: return emptyMap()
        return mapOf("local.storage_path" to JsonPrimitive(storagePath))
    }

    internal suspend fun persistAttachmentMetadata( // internal for unit testing
        attachment: Attachment,
        messageId: Long,
        channelBrand: String,
    ) {
        try {
            val localAttrs = maybeStoreLocally(attachment)
            ticketMessageAttachmentRepository.create(
                messageId    = messageId,
                kind         = attachment.kind,
                fileName     = attachment.fileName,
                contentType  = attachment.contentType,
                fileSize     = attachment.fileSize,
                channelBrand = channelBrand,
                attributes   = JsonObject(attachment.attributes + localAttrs),
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Failed to persist attachment metadata '${attachment.fileName}' for message $messageId", e)
        }
    }

}
