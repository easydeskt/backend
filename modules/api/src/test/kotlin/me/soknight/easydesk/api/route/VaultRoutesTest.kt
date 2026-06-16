@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.route

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.json.Json
import me.soknight.easydesk.api.auth.ApiAuthenticator
import me.soknight.easydesk.api.helper.TestFixtures
import me.soknight.easydesk.service.vault.domain.VaultSecret
import me.soknight.easydesk.service.vault.encryption.VaultEncryptionService
import me.soknight.easydesk.service.vault.repository.VaultSecretRepository

class VaultRoutesTest {

    private val repository = mockk<VaultSecretRepository>()
    private val encryptionService = mockk<VaultEncryptionService>()
    private val authenticator = mockk<ApiAuthenticator>()

    @BeforeTest
    fun setUp() { clearMocks(repository, encryptionService, authenticator) }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false })
        }
        routing {
            with(VaultRoutes(authenticator, encryptionService, repository)) { configureRoutes() }
        }
        block()
    }

    private fun secret(id: Long = 1L, name: String = "BOT_TOKEN") =
        VaultSecret(Instant.DISTANT_PAST, "Test secret", "encrypted_blob", id, name, Instant.DISTANT_PAST)

    // ─── GET /api/v1/vault ───────────────────────────────────────────────────────

    @Test
    fun `GET vault returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/vault").status)
    }

    @Test
    fun `GET vault returns 403 for operator`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        assertEquals(HttpStatusCode.Forbidden, client.get("/api/v1/vault").status)
    }

    @Test
    fun `GET vault returns secrets list without encrypted value`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { repository.findAll() } returns listOf(secret())

        val response = client.get("/api/v1/vault")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("BOT_TOKEN"))
        assertFalse(body.contains("encrypted_blob"))
    }

    // ─── POST /api/v1/vault ──────────────────────────────────────────────────────

    @Test
    fun `POST vault returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null
        val response = client.post("/api/v1/vault") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"KEY","value":"val"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST vault returns 403 for operator`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        val response = client.post("/api/v1/vault") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"KEY","value":"val"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST vault returns 400 when name is lowercase`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        val response = client.post("/api/v1/vault") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"lowercase","value":"val"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST vault returns 400 when name starts with digit`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        val response = client.post("/api/v1/vault") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"1KEY","value":"val"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST vault returns 400 when value is blank`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        val response = client.post("/api/v1/vault") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"VALID_KEY","value":"   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST vault returns 409 when name already exists`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        every { encryptionService.encrypt(any()) } returns "encrypted_blob"
        coEvery { repository.create("KEY", null, "encrypted_blob") } returns null

        val response = client.post("/api/v1/vault") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"KEY","value":"val"}""")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST vault returns 201 with metadata but no plaintext or encrypted value`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        every { encryptionService.encrypt("real-token") } returns "encrypted_blob"
        coEvery { repository.create("BOT_TOKEN", null, "encrypted_blob") } returns secret()

        val response = client.post("/api/v1/vault") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"BOT_TOKEN","value":"real-token"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("BOT_TOKEN"))
        assertFalse(body.contains("encrypted_blob"))
        assertFalse(body.contains("real-token"))
    }

    // ─── PUT /api/v1/vault/{id} ──────────────────────────────────────────────────

    @Test
    fun `PUT vault returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null
        val response = client.put("/api/v1/vault/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"description":"updated"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT vault returns 403 for operator`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        val response = client.put("/api/v1/vault/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"description":"updated"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT vault returns 404 when secret does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { repository.update(99L, any(), any()) } returns null

        val response = client.put("/api/v1/vault/99") {
            contentType(ContentType.Application.Json)
            setBody("""{"description":"updated"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT vault returns 400 when value is blank`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal

        val response = client.put("/api/v1/vault/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"value":"   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT vault does not re-encrypt when value is absent`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { repository.update(1L, "new desc", null) } returns secret()

        val response = client.put("/api/v1/vault/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"description":"new desc"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 0) { encryptionService.encrypt(any()) }
    }

    // ─── DELETE /api/v1/vault/{id} ───────────────────────────────────────────────

    @Test
    fun `DELETE vault returns 401 when unauthenticated`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns null
        assertEquals(HttpStatusCode.Unauthorized, client.delete("/api/v1/vault/1").status)
    }

    @Test
    fun `DELETE vault returns 403 for operator`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.operatorPrincipal
        assertEquals(HttpStatusCode.Forbidden, client.delete("/api/v1/vault/1").status)
    }

    @Test
    fun `DELETE vault returns 404 when secret does not exist`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { repository.delete(99L) } returns false

        assertEquals(HttpStatusCode.NotFound, client.delete("/api/v1/vault/99").status)
    }

    @Test
    fun `DELETE vault returns 204 on success`() = withApp {
        coEvery { authenticator.authenticate(any()) } returns TestFixtures.adminPrincipal
        coEvery { repository.delete(1L) } returns true

        assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/vault/1").status)
    }
}
