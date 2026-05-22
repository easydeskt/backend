package me.soknight.easydesk.channel.vkontakte

import me.soknight.easydesk.channel.api.ChannelActor

/**
 * A VKontakte user identity within the channel.
 *
 * The [userId] corresponds to the VK user ID (`from_id` in Community API),
 * which is a positive integer for regular users.
 *
 * @property userId VK user ID (to be replaced with SDK's inline value class)
 */
data class VKontakteIdentity(
    val userId: Long, // FIXME use SDK's inline value class here
) : ChannelActor.Identity {

    override val channelProvider get() = VKontakteProvider

    override val nativeId: String
        get() = userId.toString()

}
