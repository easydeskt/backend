@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.mockk.*
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.test.runTest
import me.soknight.easydesk.api.config.ApiConfig
import me.soknight.easydesk.api.helper.TestFixtures
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig

class ApiAuthenticatorTest {

    private val agentRepository = mockk<AgentRepository>()
    private val apiConfig = mockk<ApiConfig>()
    private val supervisorConfig = mockk<TelegramSupervisorConfig>()

    private val botToken = "test_bot_token_12345"
    private val telegramUserId = 123456789L

    @BeforeTest
    fun setUp() {
        clearMocks(agentRepository, apiConfig, supervisorConfig)
        every { apiConfig.devAuthSkip } returns false
        every { supervisorConfig.token } returns botToken
    }

    private fun makeAuthenticator() = ApiAuthenticator(agentRepository, apiConfig, supervisorConfig)

    private fun mockCall(authHeader: String?): ApplicationCall {
        val call = mockk<ApplicationCall>()
        val request = mockk<ApplicationRequest>()
        val headers = mockk<Headers>()
        every { call.request } returns request
        every { request.headers } returns headers
        every { headers["Authorization"] } returns authHeader
        return call
    }

    private fun hmacSha256(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun buildValidInitData(rawParams: Map<String, String>): String {
        val dataCheckString = rawParams
            .entries
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value}" }
        val secretKey = hmacSha256(botToken.toByteArray(Charsets.UTF_8), "WebAppData".toByteArray(Charsets.UTF_8))
        val hash = hmacSha256(dataCheckString.toByteArray(Charsets.UTF_8), secretKey).toHexString()
        val allParams = rawParams + ("hash" to hash)
        return allParams.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
    }

    private fun validParams(userId: Long = telegramUserId, username: String = "test_user"): Map<String, String> =
        mapOf(
            "auth_date" to Clock.System.now().epochSeconds.toString(),
            "query_id" to "test_query",
            "user" to """{"id":$userId,"username":"$username"}""",
        )

    private fun buildRawInitData(params: Map<String, String>): String =
        params.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }

    // --- Authorization header validation ---

    @Test
    fun `authenticate returns null when no Authorization header`() = runTest {
        val result = makeAuthenticator().authenticate(mockCall(null))
        assertNull(result)
    }

    @Test
    fun `authenticate returns null when Authorization header has wrong prefix`() = runTest {
        val result = makeAuthenticator().authenticate(mockCall("Bearer some_token"))
        assertNull(result)
    }

    @Test
    fun `authenticate returns null when Authorization header is blank after tma prefix`() = runTest {
        val result = makeAuthenticator().authenticate(mockCall("tma "))
        assertNull(result)
    }

    // --- Dev mode (devAuthSkip = true) ---

    @Test
    fun `authenticate in dev mode returns null when no agents exist`() = runTest {
        every { apiConfig.devAuthSkip } returns true
        coEvery { agentRepository.findBySupervisorBinding(any(), any()) } returns null
        coEvery { agentRepository.findAll(any()) } returns emptyList()

        val initData = buildRawInitData(mapOf("auth_date" to "12345"))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNull(result)
    }

    @Test
    fun `authenticate in dev mode returns first agent when no user id in initData`() = runTest {
        every { apiConfig.devAuthSkip } returns true
        coEvery { agentRepository.findAll(any()) } returns listOf(TestFixtures.operatorAgent)

        val initData = buildRawInitData(mapOf("auth_date" to "12345"))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNotNull(result)
        assertEquals(TestFixtures.operatorAgent, result.agent)
    }

    @Test
    fun `authenticate in dev mode finds agent by telegram binding when user id present`() = runTest {
        every { apiConfig.devAuthSkip } returns true
        coEvery { agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, telegramUserId.toString()) } returns TestFixtures.operatorAgent
        coEvery { agentRepository.patchBindingAttributes(TestFixtures.operatorId, TelegramSupervisorBrand, any()) } just Runs

        val initData = buildRawInitData(validParams(userId = telegramUserId))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNotNull(result)
        assertEquals(TestFixtures.operatorAgent, result.agent)
        assertEquals("test_user", result.telegramUsername)
    }

    @Test
    fun `authenticate in dev mode falls back to first agent when binding not found`() = runTest {
        every { apiConfig.devAuthSkip } returns true
        coEvery { agentRepository.findBySupervisorBinding(any(), any()) } returns null
        coEvery { agentRepository.findAll(any()) } returns listOf(TestFixtures.operatorAgent)

        val initData = buildRawInitData(validParams(userId = telegramUserId))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNotNull(result)
        assertEquals(TestFixtures.operatorAgent, result.agent)
    }

    @Test
    fun `authenticate in dev mode returns null when found agent is inactive`() = runTest {
        every { apiConfig.devAuthSkip } returns true
        coEvery { agentRepository.findAll(any()) } returns listOf(TestFixtures.operatorAgent.copy(isActive = false))

        val initData = buildRawInitData(mapOf("auth_date" to "12345"))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNull(result)
    }

    @Test
    fun `authenticate in dev mode returns null when bound agent is inactive`() = runTest {
        every { apiConfig.devAuthSkip } returns true
        coEvery { agentRepository.findBySupervisorBinding(any(), any()) } returns TestFixtures.operatorAgent.copy(isActive = false)

        val initData = buildRawInitData(validParams(userId = telegramUserId))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNull(result)
    }

    // --- Normal mode (devAuthSkip = false) ---

    @Test
    fun `authenticate in normal mode returns null when hash param is missing`() = runTest {
        val params = mapOf(
            "auth_date" to Clock.System.now().epochSeconds.toString(),
            "user" to URLEncoder.encode("""{"id":$telegramUserId}""", "UTF-8"),
        )
        val initData = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))
        assertNull(result)
    }

    @Test
    fun `authenticate in normal mode returns null when auth_date param is missing`() = runTest {
        val initData = "user=%7B%22id%22%3A${telegramUserId}%7D&hash=somehash"
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))
        assertNull(result)
    }

    @Test
    fun `authenticate in normal mode returns null when auth_date is older than 24 hours`() = runTest {
        val expiredDate = Clock.System.now().epochSeconds - 86_401L
        val params = mapOf(
            "auth_date" to expiredDate.toString(),
            "hash" to "fakehash",
            "user" to """{"id":$telegramUserId}""",
        )
        val initData = buildRawInitData(params)
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))
        assertNull(result)
    }

    @Test
    fun `authenticate in normal mode returns null when HMAC signature is invalid`() = runTest {
        val params = validParams().toMutableMap()
        val parts = params.entries.map { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" } + listOf("hash=0000000000000000000000000000000000000000000000000000000000000000")
        val initData = parts.joinToString("&")
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))
        assertNull(result)
    }

    @Test
    fun `authenticate in normal mode returns null when no agent binding exists`() = runTest {
        coEvery { agentRepository.findBySupervisorBinding(any(), any()) } returns null

        val initData = buildValidInitData(validParams())
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNull(result)
    }

    @Test
    fun `authenticate in normal mode returns null when bound agent is inactive`() = runTest {
        coEvery { agentRepository.findBySupervisorBinding(any(), eq(telegramUserId.toString())) } returns
            TestFixtures.operatorAgent.copy(isActive = false)

        val initData = buildValidInitData(validParams(userId = telegramUserId))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNull(result)
    }

    @Test
    fun `authenticate in normal mode returns AgentPrincipal for valid initData`() = runTest {
        coEvery { agentRepository.findBySupervisorBinding(any(), eq(telegramUserId.toString())) } returns TestFixtures.operatorAgent
        coEvery { agentRepository.patchBindingAttributes(TestFixtures.operatorId, TelegramSupervisorBrand, any()) } just Runs

        val initData = buildValidInitData(validParams(userId = telegramUserId, username = "test_user"))
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNotNull(result)
        assertEquals(TestFixtures.operatorAgent, result.agent)
        assertEquals("test_user", result.telegramUsername)
    }

    @Test
    fun `authenticate in normal mode returns AgentPrincipal with null username when user has no username`() = runTest {
        coEvery { agentRepository.findBySupervisorBinding(any(), eq(telegramUserId.toString())) } returns TestFixtures.operatorAgent

        val paramsWithoutUsername = mapOf(
            "auth_date" to Clock.System.now().epochSeconds.toString(),
            "query_id" to "test_query",
            "user" to """{"id":$telegramUserId}""",
        )
        val initData = buildValidInitData(paramsWithoutUsername)
        val result = makeAuthenticator().authenticate(mockCall("tma $initData"))

        assertNotNull(result)
        assertEquals(TestFixtures.operatorAgent, result.agent)
        assertNull(result.telegramUsername)
    }

}
