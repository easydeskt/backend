package me.soknight.easydesk.channel.api

/**
 * Describes a channel's platform identity and capabilities.
 *
 * Each channel implementation provides a singleton [ChannelBrand] that
 * uniquely identifies the platform and declares which [Feature]s it supports.
 *
 * ```kotlin
 * object TelegramBrand : ChannelBrand {
 *
 *     override val identifier = "telegram"
 *
 *     override val humanName = "Telegram"
 *
 *     override val supportedFeatures = listOf(
 *         Feature.MESSAGE_DELETE,
 *         Feature.MESSAGE_EDIT,
 *         Feature.MESSAGE_FORMAT,
 *     )
 *
 * }
 * ```
 *
 * @see ChannelProvider
 * @see me.soknight.easydesk.channel.api.state.ChannelScoped
 * @see me.soknight.easydesk.channel.api.state.AttributesHolder
 */
interface ChannelBrand {

    /** Unique machine-readable key (e.g., `"telegram"`, `"email"`, `"vkontakte"`). */
    val identifier: String

    /** Human-readable platform name shown in the UI. Defaults to [identifier]. */
    val humanName: String
        get() = identifier

    /** The set of optional [Feature]s this platform supports. */
    val supportedFeatures: List<Feature>

    /** Checks whether the given [feature] is supported by this platform. */
    fun isSupported(feature: Feature): Boolean =
        feature in supportedFeatures

    /**
     * Optional capabilities that a channel platform may or may not support.
     *
     * Use [ChannelBrand.isSupported] to check availability before invoking
     * the corresponding operation.
     */
    enum class Feature {

        /** The platform allows deleting previously sent messages. */
        MESSAGE_DELETE,

        /** The platform allows editing previously sent messages. */
        MESSAGE_EDIT,

        /** The platform supports rich text formatting (Markdown, HTML, etc.). */
        MESSAGE_FORMAT,
        ;

    }

}