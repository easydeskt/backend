package me.soknight.easydesk.channel.telegram

import me.soknight.easydesk.channel.api.ChannelBrand

/**
 * [ChannelBrand] for Telegram.
 *
 * Supports all [features][ChannelBrand.Feature]: message editing (no time limit),
 * message deletion (own — no limit, others — 48 hours in groups),
 * and rich text formatting (MarkdownV2, HTML, entities).
 */
object TelegramBrand : ChannelBrand {

    override val identifier get() = "tg"

    override val humanName get() = "Telegram"

    override val supportedFeatures: List<ChannelBrand.Feature>
        get() = ChannelBrand.Feature.entries

}