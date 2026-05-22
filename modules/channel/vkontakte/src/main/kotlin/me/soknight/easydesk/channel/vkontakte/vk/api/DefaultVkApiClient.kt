package me.soknight.easydesk.channel.vkontakte.vk.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlin.random.Random
import kotlinx.serialization.json.Json
import me.soknight.easydesk.channel.vkontakte.vk.model.VkLongPollResponse
import me.soknight.easydesk.channel.vkontakte.vk.model.VkLongPollServer
import me.soknight.easydesk.channel.vkontakte.vk.model.VkUser

internal class DefaultVkApiClient(
    private val groupId: Long,
    private val token: String,
) : VkApiClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 60_000
        }
    }

    override suspend fun deleteMessages(peerId: Long, conversationMessageIds: List<Int>) {
        post<Int>("messages.delete", parameters {
            append("cmids", conversationMessageIds.joinToString(","))
            append("delete_for_all", "1")
            append("peer_id", peerId.toString())
        })
    }

    override suspend fun editMessage(
        peerId: Long,
        conversationMessageId: Int,
        text: String,
        attachments: List<String>,
    ) {
        post<Int>("messages.edit", parameters {
            if (attachments.isNotEmpty()) append("attachment", attachments.joinToString(","))
            append("cmid", conversationMessageId.toString())
            append("message", text)
            append("peer_id", peerId.toString())
        })
    }

    override suspend fun getLongPollServer(): VkLongPollServer =
        post<LongPollServerDto>("groups.getLongPollServer", parameters {
            append("group_id", groupId.toString())
        }).toDomain()

    override suspend fun getUpdates(server: VkLongPollServer): VkLongPollResponse =
        httpClient.get(server.server) {
            parameter("act", "a_check")
            parameter("key", server.key)
            parameter("ts", server.ts)
            parameter("wait", 25)
        }.body()

    override suspend fun getUser(userId: Long): VkUser =
        getUsers(listOf(userId)).first()

    override suspend fun getUsers(userIds: List<Long>): List<VkUser> =
        post<List<VkUserDto>>("users.get", parameters {
            append("fields", "photo_200")
            append("user_ids", userIds.joinToString(","))
        }).map { it.toDomain() }

    override suspend fun sendMessage(
        peerId: Long,
        text: String,
        attachments: List<String>,
        replyTo: Int?,
    ): Int = post("messages.send", parameters {
        if (attachments.isNotEmpty()) append("attachment", attachments.joinToString(","))
        append("message", text)
        append("peer_id", peerId.toString())
        append("random_id", Random.nextLong().toString())
        replyTo?.let { append("reply_to", it.toString()) }
    })

    // ── private helpers ───────────────────────────────────────────────────────

    private suspend inline fun <reified T> post(method: String, params: io.ktor.http.Parameters): T {
        val wrapper = httpClient.submitForm(
            url = "$API_BASE/$method",
            formParameters = parameters {
                appendAll(params)
                append("access_token", token)
                append("v", API_VERSION)
            },
        ).body<VkApiResponseWrapper<T>>()
        return wrapper.response ?: error("VK API error [${wrapper.error?.code}]: ${wrapper.error?.message}")
    }

    private companion object {

        const val API_BASE = "https://api.vk.com/method"
        const val API_VERSION = "5.199"

    }

}
