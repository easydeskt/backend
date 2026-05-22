package me.soknight.easydesk.channel.vkontakte

import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelBrand.Feature

/**
 * [ChannelBrand] for VKontakte (RU).
 *
 * Supports all [features][Feature]: message editing and deletion
 * (both within a 24-hour window) and rich text formatting
 * (currently undocumented but functional).
 */
object VKontakteBrand : ChannelBrand {

    override val identifier get() = "vk"

    override val humanName get() = "VKontakte"

    override val supportedFeatures: List<Feature>
        get() = Feature.entries

}
