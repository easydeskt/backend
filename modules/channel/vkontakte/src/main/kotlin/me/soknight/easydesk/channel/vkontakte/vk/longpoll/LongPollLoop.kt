package me.soknight.easydesk.channel.vkontakte.vk.longpoll

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.soknight.easydesk.channel.vkontakte.vk.api.VkApiClient
import me.soknight.easydesk.channel.vkontakte.vk.api.toVkUpdate
import me.soknight.easydesk.channel.vkontakte.vk.model.VkUpdate
import me.soknight.easydesk.core.logging.info
import me.soknight.easydesk.core.logging.warn
import kotlin.time.Duration.Companion.seconds
import org.slf4j.Logger

internal class LongPollLoop(
    private val apiClient: VkApiClient,
    private val dispatch: suspend (VkUpdate) -> Unit,
    private val logger: Logger,
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun start(scope: CoroutineScope): Job = scope.launch {
        var server = apiClient.getLongPollServer()
        while (isActive) {
            val response = runCatching { apiClient.getUpdates(server) }.getOrElse { e ->
                logger.warn(e) { "Long Poll request failed, retrying in ${RETRY_DELAY}: ${e.message}" }
                delay(RETRY_DELAY)
                return@getOrElse null
            } ?: continue

            server = when (response.failed) {
                null -> server.copy(ts = response.ts ?: server.ts)
                1    -> server.copy(ts = response.ts ?: server.ts)
                2    -> server.copy(key = response.key ?: server.key, ts = response.ts ?: server.ts)
                else -> {
                    logger.info { "Long Poll server expired (failed=${response.failed}), reconnecting" }
                    apiClient.getLongPollServer()
                }
            }

            response.updates.forEach { rawUpdate ->
                val update = runCatching { rawUpdate.toVkUpdate(json) }.getOrElse { e ->
                    logger.warn(e) { "Failed to parse update type '${rawUpdate.type}': ${e.message}" }
                    null
                } ?: return@forEach
                runCatching { dispatch(update) }
                    .onFailure { e -> logger.warn(e) { "Handler failed for ${update::class.simpleName}: ${e.message}" } }
            }
        }
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }

}
