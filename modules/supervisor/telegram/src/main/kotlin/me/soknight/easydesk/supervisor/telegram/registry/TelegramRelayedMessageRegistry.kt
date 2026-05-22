package me.soknight.easydesk.supervisor.telegram.registry

import java.util.concurrent.ConcurrentHashMap
import org.koin.core.annotation.Single

@Single
class TelegramRelayedMessageRegistry {

    data class RelayedMessage(
        val conversationId: Long,
        val ticketId: Long,
    )

    private val map = ConcurrentHashMap<Long, RelayedMessage>()

    fun getOrNull(supervisorMessageId: Long): RelayedMessage? = map[supervisorMessageId]

    fun register(supervisorMessageId: Long, conversationId: Long, ticketId: Long) {
        map[supervisorMessageId] = RelayedMessage(conversationId = conversationId, ticketId = ticketId)
    }

}
