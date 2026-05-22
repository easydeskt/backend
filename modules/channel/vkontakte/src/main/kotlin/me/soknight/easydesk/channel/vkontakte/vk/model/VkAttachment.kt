package me.soknight.easydesk.channel.vkontakte.vk.model

import kotlinx.serialization.json.JsonObject

sealed interface VkAttachment {

    data class Audio(
        val artist: String,
        val id: Int,
        val ownerId: Long,
        val title: String,
    ) : VkAttachment

    data class AudioMessage(
        val duration: Int,
        val id: Int,
        val linkMp3: String,
        val ownerId: Long,
    ) : VkAttachment

    data class Document(
        val ext: String,
        val id: Int,
        val ownerId: Long,
        val title: String,
        val url: String?,
    ) : VkAttachment

    data class Graffiti(val id: Int, val ownerId: Long, val url: String) : VkAttachment

    data class Link(val caption: String?, val title: String, val url: String) : VkAttachment

    data class Market(
        val id: Int,
        val ownerId: Long,
        val price: String,
        val title: String,
    ) : VkAttachment

    data class Photo(
        val id: Int,
        val ownerId: Long,
        val sizes: List<Size>,
    ) : VkAttachment {

        data class Size(val height: Int, val type: String, val url: String, val width: Int)

        val largest: Size? get() = sizes.maxByOrNull { it.width * it.height }

    }

    data class Sticker(
        val images: List<Image>,
        val stickerId: Int,
    ) : VkAttachment {

        data class Image(val height: Int, val url: String, val width: Int)

    }

    data class Unknown(val raw: JsonObject, val type: String) : VkAttachment

    data class Video(
        val id: Int,
        val ownerId: Long,
        val title: String,
    ) : VkAttachment

    data class Wall(val id: Int, val ownerId: Long, val text: String?) : VkAttachment

}
