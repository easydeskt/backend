package me.soknight.easydesk.channel.vkontakte.vk.callback

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.vkontakte.vk.api.toVkUpdate
import me.soknight.easydesk.channel.vkontakte.vk.model.VkRawUpdate
import me.soknight.easydesk.channel.vkontakte.vk.model.VkUpdate
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn

private val logger = getLogger()

internal class VkCallbackConfig {
    var confirmationCode: String = ""
    var listenPath: String = "/vkontakte/callback"
    lateinit var onUpdate: suspend (VkUpdate) -> Unit
    var secret: String? = null
}

internal val VkCallbackPlugin = createApplicationPlugin("VkCallback", ::VkCallbackConfig) {
    val config = pluginConfig
    val json = Json { ignoreUnknownKeys = true }

    application.routing {
        post(config.listenPath) {
            val body = call.receiveText()
            val payload = runCatching { json.decodeFromString<CallbackPayload>(body) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            config.secret?.let { secret ->
                if (payload.secret != secret) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
            }

            when (payload.type) {
                "confirmation" -> call.respondText(config.confirmationCode)
                else -> {
                    payload.obj?.let { obj ->
                        val update = runCatching {
                            VkRawUpdate(obj, payload.type).toVkUpdate(json)
                        }.getOrElse { e ->
                            logger.warn(e) { "Failed to parse callback update '${payload.type}': ${e.message}" }
                            null
                        }
                        update?.let {
                            runCatching { config.onUpdate(it) }
                                .onFailure { e -> logger.warn(e) { "Callback handler failed: ${e.message}" } }
                        }
                    }
                    call.respondText("ok")
                }
            }
        }
    }
}

internal fun Application.installVkCallback(
    confirmationCode: String,
    listenPath: String,
    onUpdate: suspend (VkUpdate) -> Unit,
    secret: String? = null,
) {
    install(VkCallbackPlugin) {
        this.confirmationCode = confirmationCode
        this.listenPath = listenPath
        this.onUpdate = onUpdate
        this.secret = secret
    }
}

@Serializable
private data class CallbackPayload(
    @SerialName("object") val obj: JsonObject? = null,
    val secret: String? = null,
    val type: String,
)
