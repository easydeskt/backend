package me.soknight.easydesk.app.server

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.soknight.easydesk.app.server.plugin.*
import me.soknight.easydesk.channel.api.ChannelBrandRegistry
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.server.ServerModule
import me.soknight.easydesk.service.audit.event.AuditEventHandler
import me.soknight.easydesk.service.tickets.data.event.MessageEventHandler
import me.soknight.easydesk.supervisor.api.SupervisorProvider
import org.koin.ktor.ext.getKoin

private val logger = getLogger()

private lateinit var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>

/** Starts the Ktor server using [EngineMain] configuration from `application.yaml`. */
fun startServer(args: Array<String>) {
    engine = EngineMain.createServer(args)
    engine.start(wait = false)
}

/** Gracefully stops the running Ktor server. */
fun stopServer() {
    if (::engine.isInitialized) {
        engine.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
    }
}

/**
 * Application module referenced from `application.yaml`.
 *
 * Installs plugins and discovers [ServerModule] implementations via Koin.
 */
@OptIn(ExperimentalKtorApi::class)
fun Application.module() {
    configureCors()
    configureContentNegotiation()
    configureDatabase()
    configureKoin()
    configureStatusPages()
    fixSuperadminBootstrap()

    val koin = getKoin()
    val eventBus = koin.get<EventBus>()

    val channelProviders = koin.getAll<ChannelProvider>()
    val supervisorProviders = koin.getAll<SupervisorProvider>()

    channelProviders.forEach { provider ->
        launch {
            try {
                provider.start(this@module, eventBus)
            } catch (ex: NotImplementedError) {
                logger.warn("Channel provider '{}' is not yet implemented: {}", provider::class.simpleName, ex.message)
            }
        }
    }

    supervisorProviders.forEach { provider ->
        launch {
            try {
                provider.start(this@module, eventBus)
            } catch (ex: NotImplementedError) {
                logger.warn("Supervisor provider '{}' is not yet implemented: {}", provider::class.simpleName, ex.message)
            }
        }
    }

    monitor.subscribe(ApplicationStopped) {
        runBlocking {
            channelProviders.forEach { it.stop() }
            supervisorProviders.forEach { it.stop() }
        }
    }

    koin.get<AuditEventHandler>().start(this)
    koin.get<MessageEventHandler>().start(this)

    if (developmentMode) {
        registerApiKeySecurityScheme(
            name = "telegramInitData",
            keyName = "Authorization",
            keyLocation = SecuritySchemeIn.HEADER,
            description = "Telegram Mini App initData. Format: `tma <initData>`",
        )
    }

    routing {
        if (developmentMode) {
            val configureApiDoc: OpenApiDocDsl.() -> Unit = {
                info = OpenApiInfo(
                    title = "EasyDesk API",
                    version = "dev",
                    description = """
                        HTTP API consumed by the EasyDesk Telegram Mini App.<br/>
                        Every request is authenticated with Telegram initData.
                    """.trimIndent(),
                )

                servers {
                    server("https://easydesk.soknight.ru") { description = "Production server" }
                    server("http://localhost:8080") { description = "Local development" }
                }

                security { requirement("telegramInitData") }

                tag("Me")
                tag("Tickets")
                tag("Ticket notes")
                tag("Tags")
                tag("Templates")
                tag("Channels")
                tag("Agents")
                tag("Identities")
                tag("Workspace")
            }

            swaggerUI(path = "docs/swagger") {
                configureApiDoc()
            }

            val openApiSource = OpenApiDocSource.Routing(contentType = ContentType.Application.Yaml)
            val openApiBaseDoc = OpenApiDoc.Builder().apply(configureApiDoc).build()

            get("docs/openapi") {
                val doc = openApiSource.read(application, openApiBaseDoc)
                call.response.headers.append(HttpHeaders.ContentDisposition, "attachment; filename=\"easydesk-openapi.yaml\"")
                call.respondText(doc.content, doc.contentType)
            }.hide()
        }

        koin.getAll<ServerModule>().forEach { module ->
            with(module) { configureRoutes() }
        }
    }

    koin.get<ChannelBrandRegistry>().apply {
        val brandIds = brands.map { it.identifier }.toSortedSet()
        logger.info("Discovered {} channel brand(s): {}", brands.size, brandIds)
    }
}
