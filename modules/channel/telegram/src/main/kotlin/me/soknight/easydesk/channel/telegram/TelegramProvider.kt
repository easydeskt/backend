package me.soknight.easydesk.channel.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import kotlinx.coroutines.CoroutineScope
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.channel.api.dsl.channelConfigSchema
import me.soknight.easydesk.channel.telegram.internal.ChannelProviderDelegate
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Single(binds = [ChannelProvider::class])
internal class TelegramProviderBinding : ChannelProvider by TelegramProvider

/**
 * Brand/identity reference for [TelegramChannel] and [TelegramIdentity].
 *
 * This object is **not** the bound [ChannelProvider] — [TelegramProviderBinding]
 * is registered in DI instead and delegates here. Runtime behavior is provided
 * by [ChannelProviderDelegate] which is initialized lazily on first use.
 *
 * @see TelegramBrand
 * @see ChannelProviderDelegate
 * @see TelegramIdentity
 */
object TelegramProvider : ChannelProvider, KoinComponent {

    private val delegate by lazy { ChannelProviderDelegate(getLogger(), get<ChannelRepository>()) }

    override val brand get() = TelegramBrand

    override val channels: List<Channel>
        get() = delegate.channels

    override val configSchema by channelConfigSchema {
        password("token", required = true)
        url("api_url", defaultValue = "https://api.telegram.org")

        section("polling") {
            boolean("enabled", defaultValue = true)
            int("timeout_seconds", defaultValue = 30)
            long("media_groups_debounce_millis", defaultValue = 1000L)
        }

        section("webhook") {
            url("url", required = true)
            text("listen_path", defaultValue = "/telegram/webhook")
            password("secret_token")
            boolean("drop_pending_updates")
            int("max_connections", defaultValue = 40)
        }
    }

    override suspend fun start(scope: CoroutineScope, eventBus: EventBus) =
        delegate.start(scope, eventBus)

    override suspend fun stop() =
        delegate.stop()

    // -------------- INTERNAL API -------------------------------------------------------------------------------------

    internal fun getBot(serviceChannelId: Long): TelegramBot? = delegate.getBot(serviceChannelId)

}
