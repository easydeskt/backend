package me.soknight.easydesk.channel.vkontakte.vk.model

sealed interface VkUpdate {

    data class MessageAllow(val key: String, val userId: Long) : VkUpdate

    data class MessageDeny(val userId: Long) : VkUpdate

    data class MessageEdit(val message: VkMessage) : VkUpdate

    data class MessageNew(
        val clientInfo: VkClientInfo?,
        val message: VkMessage,
    ) : VkUpdate

    data class MessageRead(
        val fromId: Long,
        val isIncoming: Boolean,
        val peerId: Long,
        val readMessageId: Int,
        val unreadCount: Int,
    ) : VkUpdate

    data class MessageReaction(
        val cmid: Int,
        val fromId: Long,
        val peerId: Long,
        val reactionId: Int?,
    ) : VkUpdate

    data class MessageReply(val message: VkMessage) : VkUpdate

    data class TypingState(
        val fromId: Long,
        val state: String,
        val toId: Long,
    ) : VkUpdate

}
