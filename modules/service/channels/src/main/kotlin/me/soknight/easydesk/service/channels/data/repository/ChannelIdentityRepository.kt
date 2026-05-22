package me.soknight.easydesk.service.channels.data.repository

import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity

/**
 * Persistence contract for [ChannelIdentity] management.
 *
 * All methods run inside a suspended transaction.
 */
interface ChannelIdentityRepository {

    /**
     * Creates and persists a new channel identity.
     *
     * The pair `(channelBrand, nativeId)` must be unique; a duplicate insert will throw
     * a database constraint violation. Prefer [findOrCreate] for idempotent upserts.
     *
     * @param channelBrand platform this identity belongs to
     * @param nativeId platform-specific user id
     * @param displayName human-readable name, or `null` if unavailable
     * @return the persisted [ChannelIdentity] with its generated [id][ChannelIdentity.identifier]
     */
    suspend fun create(
        channelBrand: ChannelBrand,
        nativeId: String,
        displayName: String? = null,
    ): ChannelIdentity

    /**
     * Returns the existing identity for the given platform coordinates, or creates a new one.
     *
     * When [displayName] differs from the stored value the record is updated in place,
     * and [ChannelIdentity.lastSeenAt] is always refreshed.
     *
     * @param channelBrand platform this identity belongs to
     * @param nativeId platform-specific user id
     * @param displayName current display name from the platform
     */
    suspend fun findOrCreate(
        channelBrand: ChannelBrand,
        nativeId: String,
        displayName: String? = null,
    ): ChannelIdentity

    /**
     * Returns the identity with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): ChannelIdentity?

    /**
     * Returns the identity for the given platform coordinates, or `null` if not found.
     *
     * @param channelBrand platform this identity belongs to
     * @param nativeId platform-specific user id
     */
    suspend fun findByNativeId(channelBrand: ChannelBrand, nativeId: String): ChannelIdentity?

}
