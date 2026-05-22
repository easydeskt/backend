package me.soknight.easydesk.channel.email.internal

import org.eclipse.angus.mail.iap.BadCommandException
import org.eclipse.angus.mail.imap.IMAPFolder
import jakarta.mail.Authenticator
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.FolderClosedException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.StoreClosedException
import jakarta.mail.event.MessageCountAdapter
import jakarta.mail.event.MessageCountEvent
import jakarta.mail.search.FlagTerm
import java.util.Properties
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import me.soknight.easydesk.channel.email.EmailChannel
import me.soknight.easydesk.channel.email.domain.EmailConversation
import me.soknight.easydesk.channel.email.domain.EmailMessage
import me.soknight.easydesk.core.logging.info
import me.soknight.easydesk.core.logging.warn
import org.slf4j.Logger

internal class ImapPoller(
    private val channel: EmailChannel,
    private val logger: Logger,
) {

    private val mapper = MimeMessageMapper(channel)
    private val messageQueue = Channel<MimeMessageMapper.MappedEmail>(Channel.UNLIMITED)

    private var pollerJob: Job? = null
    @Volatile private var supportsIdle = true

    fun start(
        scope: CoroutineScope,
        onMessage: suspend (message: EmailMessage, conversation: EmailConversation, timestamp: Instant) -> Unit,
    ): Job {
        pollerJob = scope.launch(Dispatchers.IO) {
            val consumer = launch {
                for (mapped in messageQueue) {
                    try {
                        onMessage(mapped.message, mapped.conversation, mapped.timestamp)
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to process email for '${channel.humanName}': ${e.message}" }
                    }
                }
            }

            var retryDelaySeconds = INITIAL_RETRY_SECONDS
            while (isActive) {
                try {
                    connectAndListen()
                    retryDelaySeconds = INITIAL_RETRY_SECONDS
                } catch (e: Exception) {
                    if (!isActive) break
                    logger.warn(e) { "IMAP error for '${channel.humanName}', retrying in ${retryDelaySeconds}s: ${e.message}" }
                    delay(retryDelaySeconds.seconds)
                    retryDelaySeconds = (retryDelaySeconds * 2).coerceAtMost(MAX_RETRY_SECONDS)
                }
            }

            consumer.cancel()
        }
        logger.info { "Started IMAP poller for channel '${channel.humanName}'" }
        return pollerJob!!
    }

    fun stop() {
        pollerJob?.cancel()
        messageQueue.close()
        logger.info { "Stopped IMAP poller for channel '${channel.humanName}'" }
    }

    private suspend fun connectAndListen() {
        val imapConfig = channel.config.imap
        val host = imapConfig.host ?: error("IMAP host not configured for '${channel.humanName}'")
        val protocol = if (imapConfig.shouldUseSSL) "imaps" else "imap"

        val props = Properties().apply {
            put("mail.$protocol.auth", "true")
            put("mail.$protocol.connectiontimeout", "10000")
            put("mail.$protocol.host", host)
            put("mail.$protocol.port", imapConfig.port.toString())
            put("mail.$protocol.ssl.enable", imapConfig.shouldUseSSL.toString())
            put("mail.$protocol.timeout", "30000")
        }
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(imapConfig.username ?: "", imapConfig.password ?: "")
        })

        val store = session.getStore(protocol)
        store.connect(host, imapConfig.port, imapConfig.username ?: "", imapConfig.password ?: "")

        val folder = store.getFolder(imapConfig.folder).apply { open(Folder.READ_WRITE) }

        folder.addMessageCountListener(object : MessageCountAdapter() {
            override fun messagesAdded(e: MessageCountEvent) {
                e.messages.forEach { msg ->
                    try {
                        val mapped = mapper.map(msg)
                        msg.setFlag(Flags.Flag.SEEN, true)
                        messageQueue.trySend(mapped)
                    } catch (ex: Exception) {
                        logger.warn(ex) { "Failed to parse incoming email for '${channel.humanName}': ${ex.message}" }
                    }
                }
            }
        })

        sweepUnseen(folder)

        val imapFolder = folder as? IMAPFolder

        try {
            while (currentCoroutineContext().isActive) {
                if (supportsIdle && imapFolder != null) {
                    try {
                        runInterruptible { imapFolder.idle() }
                    } catch (e: FolderClosedException) {
                        throw e
                    } catch (e: StoreClosedException) {
                        throw e
                    } catch (e: BadCommandException) {
                        supportsIdle = false
                        logger.info { "IMAP IDLE not available for '${channel.humanName}': ${e.message}, switching to polling" }
                    }
                } else {
                    delay(imapConfig.pollIntervalSeconds.seconds)
                    sweepUnseen(folder)
                }
            }
        } finally {
            runCatching { folder.close(false) }
            runCatching { store.close() }
        }
    }

    private fun sweepUnseen(folder: Folder) {
        val unseen = folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
        unseen.forEach { msg ->
            try {
                val mapped = mapper.map(msg)
                msg.setFlag(Flags.Flag.SEEN, true)
                messageQueue.trySend(mapped)
            } catch (ex: Exception) {
                logger.warn(ex) { "Failed to parse UNSEEN email for '${channel.humanName}': ${ex.message}" }
            }
        }
    }

    private companion object {
        const val INITIAL_RETRY_SECONDS = 5
        const val MAX_RETRY_SECONDS = 300
    }

}
