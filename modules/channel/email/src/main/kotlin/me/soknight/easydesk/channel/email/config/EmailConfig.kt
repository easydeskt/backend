package me.soknight.easydesk.channel.email.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.soknight.easydesk.channel.api.ChannelConfig

/**
 * Configuration for the Email channel, deserialized from `mail.toml`.
 *
 * @property from sender identity for outbound emails
 * @property replyTo address that recipients' mail clients will use for replies
 * @property imap IMAP server configuration for receiving emails
 * @property smtp SMTP server configuration for sending emails
 */
@Serializable
data class EmailConfig(
    @SerialName("from") val from: From,
    @SerialName("reply_to") val replyTo: String? = null,
    @SerialName("imap") val imap: Imap,
    @SerialName("smtp") val smtp: Smtp,
) : ChannelConfig {

    /**
     * Sender identity for the `From` header of outbound emails.
     *
     * Produces a header like `From: EasyDesk Support <support@example.com>`.
     *
     * @property address email address (e.g., `"support@example.com"`)
     * @property name display name shown in mail clients (e.g., `"EasyDesk Support"`)
     */
    @Serializable
    data class From(
        @SerialName("address") val address: String,
        @SerialName("name") val name: String? = null,
    )

    /**
     * IMAP server configuration for receiving emails.
     *
     * @property host IMAP server hostname
     * @property port IMAP server port
     * @property username authentication username (usually the email address)
     * @property password authentication password
     * @property folder IMAP folder to watch (default: INBOX)
     * @property pollIntervalSeconds fallback polling interval when IMAP IDLE is unavailable
     * @property shouldUseSSL whether to use implicit TLS (IMAPS)
     */
    @Serializable
    data class Imap(
        @SerialName("host") val host: String? = null,
        @SerialName("port") val port: Int = Defaults.Imap.PORT,
        @SerialName("username") val username: String? = null,
        @SerialName("password") val password: String? = null,
        @SerialName("folder") val folder: String = Defaults.Imap.FOLDER,
        @SerialName("poll_interval_seconds") val pollIntervalSeconds: Int = Defaults.Imap.POLL_INTERVAL_SECONDS,
        @SerialName("use_ssl") val shouldUseSSL: Boolean = Defaults.Imap.USE_SSL,
    )

    /**
     * SMTP server configuration for sending emails.
     *
     * @property host SMTP server hostname
     * @property port SMTP server port
     * @property username authentication username (usually the email address)
     * @property password authentication password
     * @property shouldStartTLS whether to upgrade the connection to TLS via STARTTLS
     */
    @Serializable
    data class Smtp(
        @SerialName("host") val host: String? = null,
        @SerialName("port") val port: Int = Defaults.Smtp.PORT,
        @SerialName("username") val username: String? = null,
        @SerialName("password") val password: String? = null,
        @SerialName("start_tls") val shouldStartTLS: Boolean = Defaults.Smtp.START_TLS,
    )

    object Defaults {

        object Imap {

            const val FOLDER = "INBOX"
            const val POLL_INTERVAL_SECONDS = 30
            const val PORT = 993
            const val USE_SSL = true

        }

        object Smtp {

            const val PORT = 587
            const val START_TLS = true

        }

    }

}
