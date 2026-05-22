package me.soknight.easydesk.service.channels.registry

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelProviderRegistry
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import org.koin.core.annotation.Single

/**
 * Resolves the live [Channel] for a service-layer channel identifier.
 *
 * MVP behavior: looks up the service `channels` row, then returns the first live
 * [Channel] of the matching provider. Multi-channel-per-brand requires extending
 * the schema with an `identifier` column.
 *
 * @return `null` if the service channel row does not exist, the provider is not
 *   registered, or the provider has no live channels.
 */
@Single
class ChannelRegistry(
    private val channelRepository: ChannelRepository,
    private val providerRegistry: ChannelProviderRegistry,
) {

    suspend operator fun get(serviceChannelId: Long): Channel =
        requireNotNull(getOrNull(serviceChannelId)) {
            "no live Channel resolved for service channel id $serviceChannelId"
        }

    suspend fun getOrNull(serviceChannelId: Long): Channel? {
        val row = channelRepository.findById(serviceChannelId) ?: return null
        val provider = providerRegistry.getOrNull(row.brand) ?: return null
        return provider.channels.firstOrNull()
    }

}
