package me.soknight.easydesk.channel.telegram

import dev.inmo.tgbotapi.types.UserId
import me.soknight.easydesk.channel.api.ChannelActor

/**
 * A Telegram user identity within the channel.
 *
 * Wraps the ktgbotapi [UserId] and derives [nativeId] from
 * the underlying `chatId` (in Telegram, private chat ID equals user ID).
 *
 * @property userId ktgbotapi user identifier
 */
data class TelegramIdentity(
    val userId: UserId,
) : ChannelActor.Identity {

    override val channelProvider get() = TelegramProvider

    override val nativeId: String
        get() = userId.chatId.toString()

}
