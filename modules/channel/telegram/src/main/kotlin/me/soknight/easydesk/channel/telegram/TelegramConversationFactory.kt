package me.soknight.easydesk.channel.telegram

import dev.inmo.tgbotapi.types.toChatId
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.ConversationFactory
import me.soknight.easydesk.channel.telegram.domain.TelegramConversation
import org.koin.core.annotation.Single

/**
 * Reconstructs [TelegramConversation] instances from persisted identity data after a restart.
 *
 * @see ConversationFactory
 */
@Single
class TelegramConversationFactory : ConversationFactory {

    override val brand: ChannelBrand get() = TelegramBrand

    override suspend fun restore(
        serviceChannelId: Long,
        nativeId: String,
        attributes: Attributes,
    ): Conversation? {
        val channel = TelegramProvider.getChannel(serviceChannelId) ?: return null
        val bot = TelegramProvider.getBot(serviceChannelId) ?: return null
        return TelegramConversation(
            attributes = attributes,
            bot = bot,
            channel = channel,
            userChatId = nativeId.toLongOrNull()?.toChatId() ?: return null,
        )
    }

}
