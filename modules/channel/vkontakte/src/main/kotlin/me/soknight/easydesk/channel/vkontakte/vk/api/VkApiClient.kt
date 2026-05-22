package me.soknight.easydesk.channel.vkontakte.vk.api

import me.soknight.easydesk.channel.vkontakte.vk.model.VkLongPollResponse
import me.soknight.easydesk.channel.vkontakte.vk.model.VkLongPollServer
import me.soknight.easydesk.channel.vkontakte.vk.model.VkUser

internal interface VkApiClient {

    suspend fun deleteMessages(peerId: Long, conversationMessageIds: List<Int>)

    suspend fun editMessage(
        peerId: Long,
        conversationMessageId: Int,
        text: String = "",
        attachments: List<String> = emptyList(),
    )

    suspend fun getLongPollServer(): VkLongPollServer

    suspend fun getUpdates(server: VkLongPollServer): VkLongPollResponse

    suspend fun getUser(userId: Long): VkUser

    suspend fun getUsers(userIds: List<Long>): List<VkUser>

    suspend fun sendMessage(
        peerId: Long,
        text: String = "",
        attachments: List<String> = emptyList(),
        replyTo: Int? = null,
    ): Int

}
