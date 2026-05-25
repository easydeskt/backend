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

    suspend fun getDocUploadServer(peerId: Long, type: String = "doc"): VkDocUploadServerResponse

    suspend fun getLongPollServer(): VkLongPollServer

    suspend fun getPhotoUploadServer(peerId: Long): VkPhotoUploadServerResponse

    suspend fun getUpdates(server: VkLongPollServer): VkLongPollResponse

    suspend fun getUser(userId: Long): VkUser

    suspend fun getUsers(userIds: List<Long>): List<VkUser>

    suspend fun saveDoc(file: String): VkSavedDocResponse

    suspend fun savePhoto(server: Int, photo: String, hash: String): List<VkSavedPhotoResponse>

    suspend fun sendMessage(
        peerId: Long,
        text: String = "",
        attachments: List<String> = emptyList(),
        replyTo: Int? = null,
    ): Int

    suspend fun uploadDocBytes(uploadUrl: String, bytes: ByteArray, fileName: String): VkDocUploadResponse

    suspend fun uploadPhotoBytes(uploadUrl: String, bytes: ByteArray, fileName: String): VkPhotoUploadResponse

}
