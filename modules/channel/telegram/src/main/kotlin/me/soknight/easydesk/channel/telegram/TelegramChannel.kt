package me.soknight.easydesk.channel.telegram

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.telegram.config.TelegramConfig

class TelegramChannel(
    override val identifier: String,
    override val humanName: String = identifier,
    override val config: TelegramConfig,
) : Channel {

    override val provider get() = TelegramProvider

    override fun toString(): String =
        "$humanName (${TelegramBrand.humanName})"

}
