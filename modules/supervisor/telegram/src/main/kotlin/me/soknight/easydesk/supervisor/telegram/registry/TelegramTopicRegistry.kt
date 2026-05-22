package me.soknight.easydesk.supervisor.telegram.registry

import java.util.concurrent.ConcurrentHashMap
import org.koin.core.annotation.Single

@Single
class TelegramTopicRegistry {

    private val forward = ConcurrentHashMap<Long, Long>()
    private val reverse = ConcurrentHashMap<Long, Long>()

    operator fun get(ticketId: Long): Long =
        forward[ticketId] ?: throw IllegalArgumentException("No topic registered for ticketId=$ticketId")

    fun getOrNull(ticketId: Long): Long? = forward[ticketId]

    fun getTicketId(topicId: Long): Long? = reverse[topicId]

    fun getTopicId(ticketId: Long): Long? = forward[ticketId]

    fun register(ticketId: Long, topicId: Long) {
        forward[ticketId] = topicId
        reverse[topicId] = ticketId
    }

}
