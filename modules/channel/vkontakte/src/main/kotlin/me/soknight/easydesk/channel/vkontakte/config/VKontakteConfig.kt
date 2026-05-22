package me.soknight.easydesk.channel.vkontakte.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.channel.api.ChannelConfig

/**
 * Configuration for the VKontakte channel, deserialized from `vk.toml`.
 *
 * @property groupId community (group) ID the bot operates as
 * @property token community access token
 * @property callback Callback API configuration
 * @property longpoll Long Poll configuration
 */
@Serializable
data class VKontakteConfig(
    @SerialName("group_id") val groupId: Long,
    @SerialName("token") val token: String,
    @SerialName("callback") val callback: Callback? = null,
    @SerialName("longpoll") val longpoll: LongPoll? = null,
) : ChannelConfig {

    /**
     * Callback API configuration.
     *
     * @property confirmationCode string that the server returns to VK on confirmation requests
     * @property isEnabled whether Callback API is active
     * @property listenPath Ktor route path to handle incoming callback requests
     * @property secret secret key for verifying requests from VK
     */
    @Serializable
    data class Callback(
        @SerialName("confirmation_code") val confirmationCode: String? = null,
        @SerialName("enabled") val isEnabled: Boolean = Defaults.Callback.ENABLED,
        @SerialName("listen_path") val listenPath: String = Defaults.Callback.LISTEN_PATH,
        @SerialName("secret") val secret: String? = null,
    )

    /**
     * Long Poll configuration.
     *
     * @property isEnabled whether Long Poll is active
     * @property waitSeconds how long VK holds the connection before returning an empty response
     */
    @Serializable
    data class LongPoll(
        @SerialName("enabled") val isEnabled: Boolean = Defaults.LongPoll.ENABLED,
        @SerialName("wait_seconds") val waitSeconds: Int = Defaults.LongPoll.WAIT_SECONDS,
    )

    object Defaults {

        object Callback {

            const val ENABLED = false
            const val LISTEN_PATH = "/vkontakte/callback"

        }

        object LongPoll {

            const val ENABLED = true
            const val WAIT_SECONDS = 30

        }

    }

}
