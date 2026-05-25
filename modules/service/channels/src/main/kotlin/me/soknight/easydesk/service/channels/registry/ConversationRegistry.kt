package me.soknight.easydesk.service.channels.registry

import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.ConversationFactory
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory mapping from service `conversation.id` to the live channel:api [Conversation].
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
    private val conversationRepository: ConversationRepository,
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

    suspend operator fun get(serviceConversationId: Long): Conversation =
        requireNotNull(getOrNull(serviceConversationId)) {
            "no live Conversation cached or restorable for service conversation id $serviceConversationId"
        }

    suspend fun getOrNull(serviceConversationId: Long): Conversation? =
        map[serviceConversationId] ?: restore(serviceConversationId)

    // ── private helpers ───────────────────────────────────────────────────────

    private suspend fun restore(serviceConversationId: Long): Conversation? {
        val conv = conversationRepository.findById(serviceConversationId) ?: return null
        val identity = channelIdentityRepository.findById(conv.identityId) ?: return null
        val factory = factories.firstOrNull {
            it.brand.identifier == identity.channelProvider.brand.identifier
        } ?: return null
        val conversation = factory.restore(conv.channelId, identity.nativeId, conv.attributes) ?: return null
        map[serviceConversationId] = conversation
        return conversation
    }

}
