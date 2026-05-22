package me.soknight.easydesk.service.channels.data.domain

import kotlin.time.Clock
import kotlin.time.Instant
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.supervisor.api.model.ChannelIdentity as SupervisorChannelIdentity

/**
 * A client identity as seen on a specific channel platform.
 *
 * Identities are deduplicated by `(channelBrand, nativeId)`. The live [channelProvider] is
 * resolved from the persisted brand identifier at materialization time via
 * `ChannelProviderRegistry`.
 *
 * @param identifier internal auto-generated identifier
 * @param channelProvider the provider that manages this identity's platform
 * @param nativeId platform-specific user id (e.g. Telegram `user_id` as string)
 * @param displayName human-readable name from the platform, or `null` if unavailable
 * @param firstSeenAt timestamp of the first observed message from this identity
 * @param lastSeenAt timestamp of the most recent observed message
 */
data class ChannelIdentity(
    override val identifier: Long,
    override val channelProvider: ChannelProvider,
    override val nativeId: String,
    override val displayName: String?,
    val firstSeenAt: Instant = Clock.System.now(),
    val lastSeenAt: Instant = firstSeenAt,
) : SupervisorChannelIdentity, ChannelActor.Identity {

    override val channelBrand: ChannelBrand
        get() = channelProvider.brand

}
