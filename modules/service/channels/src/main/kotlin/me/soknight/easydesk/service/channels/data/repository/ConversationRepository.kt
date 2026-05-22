package me.soknight.easydesk.service.channels.data.repository

import me.soknight.easydesk.service.channels.data.domain.Conversation

/**
 * Persistence contract for [Conversation] management.
 *
 * All methods run inside a suspended transaction.
 */
interface ConversationRepository {

    /**
     * Creates and persists a new conversation.
     *
     * The pair `(channelId, identityId)` must be unique; prefer [findOrCreate] for
     * idempotent upserts.
     *
     * @param channelId id of the owning channel
     * @param identityId id of the client identity
     * @return the persisted [Conversation] with its generated [id][Conversation.id]
     */
    suspend fun create(channelId: Long, identityId: Long): Conversation

    /**
     * Returns the existing conversation for the given channel–identity pair, or creates one.
     *
     * @param channelId id of the owning channel
     * @param identityId id of the client identity
     */
    suspend fun findOrCreate(channelId: Long, identityId: Long): Conversation

    /**
     * Returns the conversation with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): Conversation?

    /**
     * Returns the conversation for the given channel–identity pair, or `null` if not found.
     *
     * @param channelId id of the owning channel
     * @param identityId id of the client identity
     */
    suspend fun findByChannelAndIdentity(channelId: Long, identityId: Long): Conversation?

    /**
     * Returns all conversations belonging to the given channel.
     */
    suspend fun findByChannel(channelId: Long): List<Conversation>

}
