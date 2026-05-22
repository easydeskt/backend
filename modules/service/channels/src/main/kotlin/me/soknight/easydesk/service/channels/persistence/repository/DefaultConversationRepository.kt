package me.soknight.easydesk.service.channels.persistence.repository

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.channels.data.domain.Conversation
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.channels.persistence.entity.ConversationEntity
import me.soknight.easydesk.service.channels.persistence.table.ConversationsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
internal class DefaultConversationRepository : ConversationRepository {

    override suspend fun create(channelId: Long, identityId: Long): Conversation =
        suspendTransaction {
            ConversationEntity.new {
                this.attributes = JsonObject(emptyMap())
                this.channelId = channelId
                this.createdAt = Clock.System.now()
                this.identityId = identityId
            }
        }.toDomain()

    override suspend fun findOrCreate(channelId: Long, identityId: Long): Conversation =
        suspendTransaction {
            with(ConversationsTable) {
                ConversationEntity
                    .find { (this.channelId eq channelId) and (this.identityId eq identityId) }
                    .singleOrNull()
                    ?: ConversationEntity.new {
                        this.attributes = JsonObject(emptyMap())
                        this.channelId = channelId
                        this.createdAt = Clock.System.now()
                        this.identityId = identityId
                    }
            }
        }.toDomain()

    override suspend fun findById(id: Long): Conversation? =
        suspendTransaction { ConversationEntity.findById(id) }?.toDomain()

    override suspend fun findByChannelAndIdentity(channelId: Long, identityId: Long): Conversation? =
        suspendTransaction {
            with(ConversationsTable) {
                ConversationEntity
                    .find { (this.channelId eq channelId) and (this.identityId eq identityId) }
                    .singleOrNull()
            }
        }?.toDomain()

    override suspend fun findByChannel(channelId: Long): List<Conversation> =
        suspendTransaction {
            ConversationEntity
                .find { ConversationsTable.channelId eq channelId }
                .map(ConversationEntity::toDomain)
        }

}
