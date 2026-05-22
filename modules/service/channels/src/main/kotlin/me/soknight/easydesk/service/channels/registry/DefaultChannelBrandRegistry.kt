package me.soknight.easydesk.service.channels.registry

import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelBrandRegistry
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.channel.api.ChannelProviderRegistry
import org.koin.core.annotation.Single

/**
 * Default [ChannelBrandRegistry] implementation that discovers all registered
 * [ChannelBrand] instances via [ChannelProvider]s from the Koin DI container.
 *
 * @see ChannelBrandRegistry
 */
@Single
internal class DefaultChannelBrandRegistry(providerRegistry: ChannelProviderRegistry) : ChannelBrandRegistry {

    private val byIdentifier = providerRegistry.providers.associate { it.brand.identifier to it.brand }

    override val brands: Set<ChannelBrand>
        get() = byIdentifier.values.toSet()

    override operator fun get(identifier: String): ChannelBrand =
        requireNotNull(getOrNull(identifier)) {
            "no ChannelBrand registered with identifier '$identifier', available: ${byIdentifier.keys}"
        }

    override fun getOrNull(identifier: String): ChannelBrand? =
        byIdentifier[identifier]

}