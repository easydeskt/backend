package me.soknight.easydesk.channel.vkontakte

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.vkontakte.config.VKontakteConfig

class VKontakteChannel(
    override val identifier: String,
    override val humanName: String = identifier,
    override val config: VKontakteConfig,
) : Channel {

    override val provider get() = VKontakteProvider

    override fun toString(): String =
        "$humanName (${VKontakteBrand.humanName})"

}
