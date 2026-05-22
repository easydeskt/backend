package me.soknight.easydesk.service.channels.registry

import me.soknight.easydesk.channel.api.model.Conversation
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory mapping from service `conversation.identifier` to the live channel:api [Conversation].
 *
 * Populated by `MessageEventHandler` on every inbound `MessageEvent.Received`. Entries
 * are not persisted — they are lost on restart and re-populated as new traffic arrives.
 */
@Single
class ConversationRegistry {

    private val map = ConcurrentHashMap<Long, Conversation>()

    /**
     * Associates [conversation] with [serviceConversationId], overwriting any previous entry.
     * Called by `MessageEventHandler` on every inbound message.
     */
    fun register(serviceConversationId: Long, conversation: Conversation) {
        // TODO MVP: no eviction — entries accumulate for the process lifetime
        map[serviceConversationId] = conversation
    }

    operator fun get(serviceConversationId: Long): Conversation =
        requireNotNull(map[serviceConversationId]) {
            "no live Conversation cached for service conversation id $serviceConversationId"
        }

    fun getOrNull(serviceConversationId: Long): Conversation? =
        map[serviceConversationId]

}
