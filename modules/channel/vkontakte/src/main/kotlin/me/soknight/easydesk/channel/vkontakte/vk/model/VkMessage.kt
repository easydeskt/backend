package me.soknight.easydesk.channel.vkontakte.vk.model

data class VkMessage(
    val attachments: List<VkAttachment>,
    val conversationMessageId: Int,
    val date: Long,
    val fromId: Long,
    val fwdMessages: List<VkMessage>,
    val geo: VkGeo?,
    val id: Int,
    val isOut: Boolean,
    val peerId: Long,
    val replyMessage: VkMessage?,
    val text: String,
)

data class VkGeo(
    val coordinates: Coordinates?,
    val type: String,
) {

    data class Coordinates(val latitude: Double, val longitude: Double)

}

data class VkClientInfo(
    val buttonActions: List<String>,
    val carousel: Boolean,
    val inlineKeyboard: Boolean,
    val keyboard: Boolean,
    val langId: Int,
)
