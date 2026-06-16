package me.soknight.easydesk.service.vault.resolver

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import me.soknight.easydesk.service.vault.domain.VaultSecret
import me.soknight.easydesk.service.vault.encryption.VaultEncryptionService
import me.soknight.easydesk.service.vault.repository.VaultSecretRepository

class SecretReferenceResolverTest {

    private val repository = mockk<VaultSecretRepository>()
    private val encryptionService = mockk<VaultEncryptionService>()
    private val resolver = SecretReferenceResolver(encryptionService, repository)

    private fun secret(id: Long, name: String, encrypted: String) =
        VaultSecret(Instant.DISTANT_PAST, null, encrypted, id, name, Instant.DISTANT_PAST)

    @BeforeTest
    fun setUp() { clearMocks(repository, encryptionService) }

    @Test
    fun `should_resolve_single_reference`() = runTest {
        coEvery { repository.findByName("BOT_TOKEN") } returns secret(1, "BOT_TOKEN", "enc1")
        every { encryptionService.decrypt("enc1") } returns "real-token"

        val result = resolver.resolve("""{"token":"${'$'}BOT_TOKEN"}""")

        assertEquals("""{"token":"real-token"}""", result)
    }

    @Test
    fun `should_leave_unresolved_reference_unchanged`() = runTest {
        coEvery { repository.findByName("MISSING") } returns null

        val result = resolver.resolve("""{"token":"${'$'}MISSING"}""")

        assertEquals("""{"token":"${'$'}MISSING"}""", result)
    }

    @Test
    fun `should_return_text_unchanged_when_no_references_present`() = runTest {
        val result = resolver.resolve("""{"token":"plain-value"}""")

        assertEquals("""{"token":"plain-value"}""", result)
        coVerify(exactly = 0) { repository.findByName(any()) }
    }

    @Test
    fun `should_resolve_multiple_different_references`() = runTest {
        coEvery { repository.findByName("HOST") } returns secret(1, "HOST", "enc_host")
        coEvery { repository.findByName("PASS") } returns secret(2, "PASS", "enc_pass")
        every { encryptionService.decrypt("enc_host") } returns "mail.example.com"
        every { encryptionService.decrypt("enc_pass") } returns "s3cr3t"

        val result = resolver.resolve("""{"host":"${'$'}HOST","password":"${'$'}PASS"}""")

        assertEquals("""{"host":"mail.example.com","password":"s3cr3t"}""", result)
    }

    @Test
    fun `should_not_match_env_var_brace_syntax`() = runTest {
        // ${VAR} is the existing env-var pattern used in channel providers; vault uses $VAR without braces
        val input = """{"token":"${'$'}{NOT_VAULT}"}"""
        val result = resolver.resolve(input)

        assertEquals(input, result)
        coVerify(exactly = 0) { repository.findByName(any()) }
    }

    @Test
    fun `should_deduplicate_repeated_references`() = runTest {
        coEvery { repository.findByName("TOKEN") } returns secret(1, "TOKEN", "enc1")
        every { encryptionService.decrypt("enc1") } returns "value"

        resolver.resolve("""{"a":"${'$'}TOKEN","b":"${'$'}TOKEN"}""")

        // findByName called once despite two occurrences in the text
        coVerify(exactly = 1) { repository.findByName("TOKEN") }
    }
}
