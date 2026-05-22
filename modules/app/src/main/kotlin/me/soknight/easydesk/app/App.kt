package me.soknight.easydesk.app

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import me.soknight.easydesk.app.server.startServer
import me.soknight.easydesk.app.server.stopServer
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.shell.EasyDeskShell
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.logging.Level
import java.util.logging.LogManager

@Module
@ComponentScan("me.soknight.easydesk")
class EasyDeskModule

@KoinApplication(modules = [EasyDeskModule::class])
object EasyDeskApp

fun main(args: Array<String>) {
    System.setProperty("slf4j.internal.verbosity", "ERROR")
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    LogManager.getLogManager().getLogger("").level = Level.ALL

    KSLog.default = KSLog { level, tag, message, throwable ->
        val logger = LoggerFactory.getLogger(tag ?: "KSLog")
        val msg = message.toString()

        when (level) {
            LogLevel.TRACE -> logger.trace(msg, throwable)
            LogLevel.DEBUG -> logger.debug(msg, throwable)
            LogLevel.VERBOSE -> logger.debug(msg, throwable)
            LogLevel.INFO -> logger.info(msg, throwable)
            LogLevel.WARNING -> logger.warn(msg, throwable)
            LogLevel.ERROR, LogLevel.ASSERT -> logger.error(msg, throwable)
        }
    }

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        getLogger().error("Uncaught exception in thread '${thread.name}'", throwable)
    }

    Runtime.getRuntime().addShutdownHook(Thread(::shutdown, "Shutdown Thread"))

    startServer(args)
    EasyDeskShell.start()
}

private fun shutdown() {
    EasyDeskShell.stop()

    getLogger().apply {
        info("Shutting down Ktor server...")
        stopServer()

        info("Goodbye!")
    }
}
