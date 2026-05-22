package me.soknight.easydesk.service.channels.persistence.entity

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.channels.data.domain.Conversation
import me.soknight.easydesk.service.channels.persistence.table.ConversationsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import kotlin.time.Instant

internal class ConversationEntity(id: EntityID<Long>) : LongEntity(id) {

    var attributes  : JsonObject by ConversationsTable.attributes
    var channelId   : Long by ConversationsTable.channelId
    var createdAt   : Instant by ConversationsTable.createdAt
    var identityId  : Long by ConversationsTable.identityId

    fun toDomain() = Conversation(
        attributes = attributes,
        channelId = channelId,
        createdAt = createdAt,
        id = id.value,
        identityId = identityId,
    )

    companion object : LongEntityClass<ConversationEntity>(ConversationsTable)

}
