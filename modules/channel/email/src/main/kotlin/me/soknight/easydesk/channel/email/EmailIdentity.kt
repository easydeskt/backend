package me.soknight.easydesk.channel.email

import me.soknight.easydesk.channel.api.ChannelActor

/**
 * An email user identity within the channel.
 *
 * Uses the email [address] from the `From` header as both
 * [nativeId] and [humanName].
 *
 * @property address email address (e.g., `"user@example.com"`)
 */
data class EmailIdentity(
    val address: String,
) : ChannelActor.Identity {

    override val channelProvider get() = EmailProvider

    override val nativeId get() = address

    override val humanName get() = address

}
