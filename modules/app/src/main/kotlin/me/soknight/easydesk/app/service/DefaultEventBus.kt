package me.soknight.easydesk.app.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.soknight.easydesk.core.event.Event
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.debug
import me.soknight.easydesk.core.logging.getLogger
import org.koin.core.annotation.Single

/**
 * Default [me.soknight.easydesk.core.event.EventBus] implementation backed by a [kotlinx.coroutines.flow.MutableSharedFlow].
 *
 * Created in the application module and passed to channel providers
 * via [me.soknight.easydesk.channel.api.ChannelProvider.start].
 *
 * @see me.soknight.easydesk.core.event.EventBus
 */
@Single
internal class DefaultEventBus : EventBus {

    private val logger = getLogger()

    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = BUFFER_CAPACITY,
    )

    override val events: SharedFlow<Event> =
        _events.asSharedFlow()

    override suspend fun publish(event: Event) {
        logger.debug { "Publishing event: $event" }
        _events.emit(event)
    }

    private companion object {
        private const val BUFFER_CAPACITY = 256
    }

}