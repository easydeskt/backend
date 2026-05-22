package me.soknight.easydesk.service.channels.data.repository

import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.channels.data.domain.Channel

/**
 * Persistence contract for [Channel] management.
 *
 * All methods run inside a suspended transaction.
 */
interface ChannelRepository {

    /**
     * Creates and persists a new channel in the enabled state.
     *
     * @param brand platform identifier (e.g. `"telegram"`)
     * @param displayName human-readable label
     * @param config platform-specific configuration blob
     * @return the persisted [Channel] with its generated [id][Channel.id]
     */
    suspend fun create(
        brand: String,
        displayName: String,
        config: JsonObject? = null,
    ): Channel

    /**
     * Returns all channels, optionally filtered to enabled ones only.
     *
     * @param enabledOnly when `true` (default), excludes disabled channels
     */
    suspend fun findAll(enabledOnly: Boolean = true): List<Channel>

    /**
     * Returns the channel with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): Channel?

    /**
     * Returns all channels with the given [brand].
     *
     * @param brand platform identifier to filter by
     * @param enabledOnly when `true` (default), excludes disabled channels
     */
    suspend fun findByBrand(brand: String, enabledOnly: Boolean = true): List<Channel>

    /**
     * Updates mutable fields of an existing channel.
     * Returns `null` if no channel with [id] exists.
     *
     * @param id id of the channel to update
     * @param displayName new label, or `null` to keep the current one
     * @param config new configuration blob, or `null` to keep the current one
     * @param isEnabled new enabled state, or `null` to keep the current one
     */
    suspend fun update(
        id: Long,
        displayName: String? = null,
        config: JsonObject? = null,
        isEnabled: Boolean? = null,
    ): Channel?

}
