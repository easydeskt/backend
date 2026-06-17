package me.soknight.easydesk.channel.telegram.config

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.channel.api.ChannelConfig

/**
 * Configuration for the Telegram channel, deserialized from `tg.toml`.
 *
 * @property apiUrl custom Bot API URL (for local Bot API server)
 * @property token bot token from [@BotFather](https://t.me/BotFather)
 * @property polling long polling configuration
 * @property webhook webhook configuration
 */
@Serializable
data class TelegramConfig(
    @SerialName("api_url") val apiUrl: String = Defaults.API_URL,
    @SerialName("token") val token: String? = null,
    @SerialName("polling") val polling: Polling? = null,
    @SerialName("webhook") val webhook: Webhook? = null,
) : ChannelConfig {

    /**
     * Long polling configuration.
     *
     * @property isEnabled whether long polling is active
     * @property mediaGroupsDebounceMillis debounce window in millis for aggregating media groups
     * @property timeoutSeconds long polling timeout in seconds (Telegram recommends 30)
     */
    @Serializable
    data class Polling(
        @SerialName("enabled") val isEnabled: Boolean = Defaults.Polling.ENABLED,
        @SerialName("media_groups_debounce_millis") val mediaGroupsDebounceMillis: Long = Defaults.Polling.MEDIA_GROUPS_DEBOUNCE_MILLIS,
        @SerialName("timeout_seconds") val timeoutSeconds: Int = Defaults.Polling.TIMEOUT_SECONDS,
    )

    /**
     * Webhook configuration.
     *
     * @property isDropPendingUpdates drop queued updates when registering the webhook
     * @property isEnabled whether webhook mode is active
     * @property listenPath Ktor route path to handle incoming webhook requests
     * @property maxConnections max simultaneous connections from Telegram (1–100)
     * @property secretToken verification token sent by Telegram
     * @property url public HTTPS URL that Telegram will POST updates to
     */
    @Serializable
    data class Webhook(
        @SerialName("drop_pending_updates") val isDropPendingUpdates: Boolean = Defaults.Webhook.DROP_PENDING_UPDATES,
        @SerialName("enabled") val isEnabled: Boolean = Defaults.Webhook.ENABLED,
        @SerialName("listen_path") val listenPath: String = Defaults.Webhook.LISTEN_PATH,
        @SerialName("max_connections") val maxConnections: Int = Defaults.Webhook.MAX_CONNECTIONS,
        @SerialName("secret_token") val secretToken: String? = null,
        @SerialName("url") val url: Url? = null,
    )

    object Defaults {

        const val API_URL = "https://api.telegram.org"

        object Polling {

            const val ENABLED = true
            const val MEDIA_GROUPS_DEBOUNCE_MILLIS = 1000L
            const val TIMEOUT_SECONDS = 30

        }

        object Webhook {

            const val DROP_PENDING_UPDATES = false
            const val ENABLED = false
            const val LISTEN_PATH = "/telegram/webhook"
            const val MAX_CONNECTIONS = 40

        }

    }

}