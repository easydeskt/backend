package me.soknight.easydesk.service.channels.persistence.repository

import kotlin.time.Clock
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelProviderRegistry
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.persistence.entity.ChannelIdentityEntity
import me.soknight.easydesk.service.channels.persistence.table.ChannelIdentitiesTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single

@Single
internal class DefaultChannelIdentityRepository(
    private val providerRegistry: ChannelProviderRegistry,
) : ChannelIdentityRepository {

    // throws IAE (from providerRegistry[]) if brand was removed from Koin config after data was stored
    private fun ChannelIdentityEntity.toFullDomain(): ChannelIdentity =
        toDomain(providerRegistry[channelBrand])

    override suspend fun create(
        channelBrand: ChannelBrand,
        nativeId: String,
        displayName: String?,
    ): ChannelIdentity = suspendTransaction {
        val now = Clock.System.now()
        ChannelIdentityEntity.new {
            this.channelBrand = channelBrand.identifier
            this.displayName = displayName
            this.firstSeenAt = now
            this.lastSeenAt = now
            this.nativeId = nativeId
        }
    }.toFullDomain()

    override suspend fun findOrCreate(
        channelBrand: ChannelBrand,
        nativeId: String,
        displayName: String?,
    ): ChannelIdentity = suspendTransaction {
        val existing = with(ChannelIdentitiesTable) {
            ChannelIdentityEntity
                .find { (this.channelBrand eq channelBrand.identifier) and (this.nativeId eq nativeId) }
                .singleOrNull()
        }

        if (existing != null) {
            existing.lastSeenAt = Clock.System.now()

            if (existing.displayName != displayName)
                existing.displayName = displayName

            existing
        } else {
            val now = Clock.System.now()
            ChannelIdentityEntity.new {
                this.channelBrand = channelBrand.identifier
                this.displayName = displayName
                this.firstSeenAt = now
                this.lastSeenAt = now
                this.nativeId = nativeId
            }
        }
    }.toFullDomain()

    override suspend fun findById(id: Long): ChannelIdentity? =
        suspendTransaction { ChannelIdentityEntity.findById(id) }?.toFullDomain()

    override suspend fun findByNativeId(channelBrand: ChannelBrand, nativeId: String): ChannelIdentity? =
        suspendTransaction {
            with(ChannelIdentitiesTable) {
                ChannelIdentityEntity
                    .find { (this.channelBrand eq channelBrand.identifier) and (this.nativeId eq nativeId) }
                    .singleOrNull()
            }
        }?.toFullDomain()

}
