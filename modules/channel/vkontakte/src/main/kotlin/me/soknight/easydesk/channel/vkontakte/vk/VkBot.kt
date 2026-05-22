package me.soknight.easydesk.channel.vkontakte.vk

import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import me.soknight.easydesk.channel.vkontakte.vk.api.DefaultVkApiClient
import me.soknight.easydesk.channel.vkontakte.vk.api.VkApiClient
import me.soknight.easydesk.channel.vkontakte.vk.callback.installVkCallback
import me.soknight.easydesk.channel.vkontakte.vk.longpoll.LongPollLoop
import me.soknight.easydesk.core.logging.getLogger

class VkBot internal constructor(
    val groupId: Long,
    internal val apiClient: VkApiClient,
)

fun vkBot(groupId: Long, token: String): VkBot =
    VkBot(groupId, DefaultVkApiClient(groupId, token))

fun VkBot.buildBehaviourWithLongPolling(
    scope: CoroutineScope,
    block: VkBehaviourContext.() -> Unit,
): Job {
    val context = VkBehaviourContext(this).apply(block)
    return LongPollLoop(apiClient, context::dispatch, getLogger()).start(scope)
}

fun VkBot.buildBehaviourWithCallbackApi(
    application: Application,
    confirmationCode: String,
    listenPath: String,
    secret: String? = null,
    block: VkBehaviourContext.() -> Unit,
) {
    val context = VkBehaviourContext(this).apply(block)
    application.installVkCallback(
        confirmationCode = confirmationCode,
        listenPath = listenPath,
        onUpdate = context::dispatch,
        secret = secret,
    )
}
