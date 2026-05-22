package me.soknight.easydesk.channel.api.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.event.ChannelEvent
import me.soknight.easydesk.channel.api.event.MessageEvent
import me.soknight.easydesk.core.event.EventBus

/** All [MessageEvent]s from any channel. */
fun EventBus.messages(): Flow<MessageEvent> =
    events.filterIsInstance()

/** Only [MessageEvent.Received] — new inbound messages. */
fun EventBus.messagesReceived(): Flow<MessageEvent.Received> =
    events.filterIsInstance()

/** Only [MessageEvent.Sent] — outbound messages confirmed by the platform. */
fun EventBus.messagesSent(): Flow<MessageEvent.Sent> =
    events.filterIsInstance()

/** Only [MessageEvent.Edited] — message edits. */
fun EventBus.messagesEdited(): Flow<MessageEvent.Edited> =
    events.filterIsInstance()

/** Only [MessageEvent.Deleted] — message deletions. */
fun EventBus.messagesDeleted(): Flow<MessageEvent.Deleted> =
    events.filterIsInstance()

/** All events from a specific platform [brand]. */
fun EventBus.fromBrand(brand: ChannelBrand): Flow<ChannelEvent> =
    events.filterIsInstance<ChannelEvent>().filter { it.channelBrand == brand }
