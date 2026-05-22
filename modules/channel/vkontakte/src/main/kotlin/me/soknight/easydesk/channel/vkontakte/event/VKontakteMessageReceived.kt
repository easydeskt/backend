package me.soknight.easydesk.channel.vkontakte.event

import kotlin.time.Instant
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.event.MessageEvent
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.vkontakte.VKontakteConversation

data class VKontakteMessageReceived(
    override val conversation: VKontakteConversation,
    override val message: Message,
    override val timestamp: Instant,
) : MessageEvent.Received {

    override val channel: Channel get() = conversation.channel

}
