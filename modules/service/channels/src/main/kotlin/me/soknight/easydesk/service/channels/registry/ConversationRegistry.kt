package me.soknight.easydesk.service.channels.registry

import kotlinx.coroutines.runBlocking
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.ConversationFactory
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory mapping from service `conversation.identifier` to the live channel:api [Conversation].
 *
 * Populated by `MessageEventHandler` on every inbound `MessageEvent.Received`. On cache miss,
 * attempts to reconstruct the conversation from the database using the registered [ConversationFactory]
 * instances, allowing agent replies to succeed after a restart even before new traffic arrives.
 *
 * @see ConversationFactory
 */
@Single
class ConversationRegistry(
    private val channelIdentityRepository: ChannelIdentityRepository,
    private val factories: List<ConversationFactory>,
) {

    private val map = ConcurrentHashMap<Long, Conversation>()

    /**
     * Associates [conversation] with [serviceConversationId], overwriting any previous entry.
     * Called by `MessageEventHandler` on every inbound message.
     */
    fun register(serviceConversationId: Long, conversation: Conversation) {
        map[serviceConversationId] = conversation
    }

    operator fun get(serviceConversationId: Long): Conversation =
        requireNotNull(getOrNull(serviceConversationId)) {
            "no live Conversation cached or restorable for service conversation id $serviceConversationId"
        }

    fun getOrNull(serviceConversationId: Long): Conversation? =
        map[serviceConversationId] ?: restoreBlocking(serviceConversationId)

    // ── private helpers ───────────────────────────────────────────────────────

    private fun restoreBlocking(serviceConversationId: Long): Conversation? =
        runBlocking { restore(serviceConversationId) }

    private suspend fun restore(serviceConversationId: Long): Conversation? {
        val identity = channelIdentityRepository.findById(serviceConversationId) ?: return null
        val factory = factories.firstOrNull {
            it.brand.identifier == identity.channelProvider.brand.identifier
        } ?: return null
        val channel = identity.channelProvider.channels.firstOrNull() ?: return null
        val conversation = factory.restore(channel, identity.nativeId, emptyMap()) ?: return null
        map[serviceConversationId] = conversation
        return conversation
    }

}
