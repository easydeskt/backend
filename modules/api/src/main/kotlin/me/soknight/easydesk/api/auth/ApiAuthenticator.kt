@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.auth

import io.ktor.server.application.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import me.soknight.easydesk.api.config.ApiConfig
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import org.koin.core.annotation.Single
import java.net.URLDecoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Clock

@Single
class ApiAuthenticator(
    private val agentRepository: AgentRepository,
    private val apiConfig: ApiConfig,
    private val config: TelegramSupervisorConfig,
) {

    private val logger = getLogger()

    init {
        if (apiConfig.devAuthSkip) {
            logger.warn { "Telegram initData HMAC validation is disabled in DEV environment!" }
        }
    }

    suspend fun authenticate(call: ApplicationCall): AgentPrincipal? {
        val rawInitData = call.request.headers["Authorization"]
            ?.takeIf { it.startsWith("tma ") }
            ?.removePrefix("tma ")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return if (apiConfig.devAuthSkip) authenticateDev(rawInitData) else authenticateNormal(rawInitData)
    }

    private suspend fun authenticateDev(rawInitData: String): AgentPrincipal? {
        val params = parseParams(rawInitData)
        val byBinding = extractUserId(params)?.let {
            agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, it.toString())
        }
        val agent = byBinding ?: agentRepository.findAll().firstOrNull() ?: return null
        if (!agent.isActive) return null
        val username = extractUsername(params)
        if (byBinding != null && username != null) updateUsername(agent.identifier, username)
        return AgentPrincipal(agent, username)
    }

    private suspend fun authenticateNormal(rawInitData: String): AgentPrincipal? {
        val telegramUserId = validateInitData(rawInitData, config.token) ?: return null
        val agent = agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, telegramUserId.toString())
            ?: return null
        if (!agent.isActive) return null
        val username = extractUsername(parseParams(rawInitData))
        if (username != null) updateUsername(agent.identifier, username)
        return AgentPrincipal(agent, username)
    }

    private fun extractUserId(params: Map<String, String>): Long? =
        params["user"]?.let { Json.parseToJsonElement(it).jsonObject["id"]?.jsonPrimitive?.longOrNull }

    private fun extractUsername(params: Map<String, String>): String? =
        params["user"]?.let { Json.parseToJsonElement(it).jsonObject["username"]?.jsonPrimitive?.content }

    private fun hmacSha256(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun parseParams(rawInitData: String): Map<String, String> =
        rawInitData.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) null
            else pair.substring(0, idx) to URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
        }.toMap()

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private suspend fun updateUsername(agentId: Uuid, username: String) {
        runCatching {
            agentRepository.patchBindingAttributes(
                agentId = agentId,
                brand = TelegramSupervisorBrand,
                patch = JsonObject(mapOf("username" to JsonPrimitive(username))),
            )
        }.onFailure { logger.warn { "Failed to update Telegram username for agent $agentId: ${it.message}" } }
    }

    private fun validateInitData(rawInitData: String, botToken: String): Long? {
        val params = parseParams(rawInitData)
        val hash = params["hash"] ?: return null
        val authDate = params["auth_date"]?.toLongOrNull() ?: return null
        // reject tokens older than 24 hours to limit replay window
        if (Clock.System.now().epochSeconds - authDate > 86_400L) return null
        val dataCheckString = params
            .filterKeys { it != "hash" }
            .entries
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value}" }
        val secretKey = hmacSha256(
            data = botToken.toByteArray(Charsets.UTF_8),
            key = "WebAppData".toByteArray(Charsets.UTF_8),
        )
        val computed = hmacSha256(
            data = dataCheckString.toByteArray(Charsets.UTF_8),
            key = secretKey,
        ).toHexString()
        if (!MessageDigest.isEqual(computed.toByteArray(), hash.lowercase().toByteArray())) return null
        return extractUserId(params)
    }

}
