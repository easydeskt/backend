package me.soknight.easydesk.channel.vkontakte.vk.model

data class VkUser(
    val firstName: String,
    val id: Long,
    val lastName: String,
    val photo200: String?,
) {
    val fullName: String get() = "$firstName $lastName".trim()
}
