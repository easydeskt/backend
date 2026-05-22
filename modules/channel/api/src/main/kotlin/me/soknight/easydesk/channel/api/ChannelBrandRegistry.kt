package me.soknight.easydesk.channel.api

/**
 * Provides access to all registered [ChannelBrand] instances.
 *
 * The registry is populated from the DI container at startup — every
 * [ChannelBrand] registered as a singleton is included. Use it to resolve
 * a platform identifier (e.g. stored in the database) back to its corresponding
 * [ChannelBrand] object.
 *
 * ```kotlin
 * val brand = registry["tg"]           // TelegramBrand, throws if absent
 * val same  = registry.getOrNull("tg") // TelegramBrand or null
 * ```
 *
 * @see ChannelBrand
 */
interface ChannelBrandRegistry {

    /** All brands currently registered in the system. */
    val brands: Set<ChannelBrand>

    /**
     * Returns the [ChannelBrand] whose [identifier][ChannelBrand.identifier] equals [identifier],
     * or throws [IllegalArgumentException] if no registered brand matches.
     *
     * @throws IllegalArgumentException if no brand with [identifier] is registered
     */
    operator fun get(identifier: String): ChannelBrand

    /**
     * Returns the [ChannelBrand] whose [identifier][ChannelBrand.identifier] equals [identifier],
     * or `null` if no registered brand matches.
     */
    fun getOrNull(identifier: String): ChannelBrand?

}
