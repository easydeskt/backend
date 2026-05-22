package me.soknight.easydesk.core.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.encoder.Encoder
import me.soknight.easydesk.core.shell.EasyDeskShell

/**
 * Logback appender that routes formatted log messages through [me.soknight.easydesk.core.shell.EasyDeskShell.printAbove],
 * keeping the prompt line fixed at the bottom of the terminal.
 */
internal class ShellLogbackAppender : AppenderBase<ILoggingEvent>() {

    lateinit var encoder: Encoder<ILoggingEvent>

    override fun append(event: ILoggingEvent) {
        val message = String(encoder.encode(event)).trimEnd('\n', '\r')
        EasyDeskShell.printAbove(message)
    }

}