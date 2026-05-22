package me.soknight.easydesk.channel.api.state

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelProvider

/**
 * Marker interface for objects that exist within the scope of a specific [Channel].
 *
 * Provides access to the [Channel] (connection), and by extension to the
 * [ChannelProvider] and [ChannelBrand].
 *
 * @see Channel
 * @see ChannelProvider
 * @see ChannelBrand
 */
interface ChannelScoped {

    /** The channel (connection) this object belongs to. */
    val channel: Channel

    /** The brand of the channel. Derived from [channel]. */
    val channelBrand: ChannelBrand
        get() = channel.provider.brand

    /** The channel provider. Derived from [channel]. */
    val channelProvider: ChannelProvider
        get() = channel.provider

}
