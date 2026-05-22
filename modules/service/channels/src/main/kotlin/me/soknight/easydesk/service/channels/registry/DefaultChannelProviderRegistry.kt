package me.soknight.easydesk.service.channels.registry

import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.channel.api.ChannelProviderRegistry
import org.koin.core.annotation.Single

/**
 * Default [ChannelProviderRegistry] implementation that discovers all registered
 * [ChannelProvider] instances from the Koin DI container.
 *
 * @see ChannelProviderRegistry
 */
@Single
internal class DefaultChannelProviderRegistry(providers: List<ChannelProvider>) : ChannelProviderRegistry {

    private val byBrand = providers.associateBy { it.brand.identifier }

    override val providers: Set<ChannelProvider>
        get() = byBrand.values.toSet()

    override operator fun get(brand: String): ChannelProvider =
        requireNotNull(getOrNull(brand)) {
            "no ChannelProvider registered for brand '$brand', available: ${byBrand.keys}"
        }

    override fun getOrNull(brand: String): ChannelProvider? =
        byBrand[brand]

}
