package me.soknight.easydesk.supervisor.api.model

import me.soknight.easydesk.channel.api.ChannelBrand

/**
 * Read-only view of a channel identity (customer contact) for supervisor surfaces.
 */
interface ChannelIdentity {

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** The platform this identity belongs to. */
    val channelBrand: ChannelBrand

    /** Human-readable name from the platform, or `null` if unavailable. */
    val displayName: String?

    /** Platform-specific user id (e.g. Telegram `user_id` as string). */
    val nativeId: String

}
