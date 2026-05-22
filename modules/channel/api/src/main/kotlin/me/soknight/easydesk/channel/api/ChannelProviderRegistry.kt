package me.soknight.easydesk.channel.api

/**
 * Provides access to all registered [ChannelProvider] instances.
 *
 * The registry is populated from the DI container at startup — every
 * [ChannelProvider] registered as a singleton is included. Use it to resolve
 * a platform identifier (e.g. stored in the database) back to its corresponding
 * [ChannelProvider] object.
 *
 * ```kotlin
 * val provider = registry["tg"]           // TelegramProvider, throws if absent
 * val same     = registry.getOrNull("tg") // TelegramProvider or null
 * ```
 *
 * @see ChannelProvider
 */
interface ChannelProviderRegistry {

    /** All providers currently registered in the system. */
    val providers: Set<ChannelProvider>

    /**
     * Returns the [ChannelProvider] whose [brand's][ChannelProvider.brand]
     * [identifier][ChannelBrand.identifier] equals [brand],
     * or throws [IllegalArgumentException] if no registered provider matches.
     *
     * @throws IllegalArgumentException if no provider for [brand] is registered
     */
    operator fun get(brand: String): ChannelProvider

    /**
     * Returns the [ChannelProvider] whose [brand's][ChannelProvider.brand]
     * [identifier][ChannelBrand.identifier] equals [brand],
     * or `null` if no registered provider matches.
     */
    fun getOrNull(brand: String): ChannelProvider?

}
