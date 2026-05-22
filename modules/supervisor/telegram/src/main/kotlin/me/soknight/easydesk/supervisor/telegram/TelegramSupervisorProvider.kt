@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.types.chat.ExtendedPrivateChat
import dev.inmo.tgbotapi.types.toChatId
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.info
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.api.SupervisorBrand
import me.soknight.easydesk.supervisor.api.SupervisorProvider
import me.soknight.easydesk.supervisor.api.model.Agent as SupervisorAgent
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.handler.TelegramAgentReplyHandler
import me.soknight.easydesk.supervisor.telegram.handler.TelegramCommandDispatcher
import me.soknight.easydesk.supervisor.telegram.handler.TelegramMessageRelayHandler
import me.soknight.easydesk.supervisor.telegram.handler.TelegramTicketEventHandler
import org.koin.core.annotation.Single

@Single(binds = [SupervisorProvider::class])
class TelegramSupervisorProvider(
    private val agentReplyHandler: TelegramAgentReplyHandler,
    private val agentRepository: AgentRepository,
    private val commandDispatcher: TelegramCommandDispatcher,
    private val config: TelegramSupervisorConfig,
    private val messageRelayHandler: TelegramMessageRelayHandler,
    private val ticketEventHandler: TelegramTicketEventHandler,
) : SupervisorProvider {

    override val brand: SupervisorBrand get() = TelegramSupervisorBrand

    private val logger = getLogger()

    private var pollingJob: Job? = null

    private suspend fun bootstrapSuperadmin(bot: TelegramBot) {
        val superadminId = config.superadminId.toString()
        val existing = agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, superadminId)
        if (existing != null) {
            logger.info { "Superadmin already bootstrapped: ${existing.displayName} (${existing.identifier})" }
            return
        }
        val displayName = try {
            val chat = bot.getChat(config.superadminId.toChatId())
            (chat as? ExtendedPrivateChat)?.firstName ?: "Superadmin"
        } catch (e: CommonRequestException) {
            logger.warn("Could not fetch superadmin chat ({}), using fallback name — start a dialog with the bot to resolve this", e.response.description)
            "Superadmin"
        }
        val agent = agentRepository.create(displayName = displayName, role = SupervisorAgent.Role.ADMIN)
        agentRepository.linkSupervisor(agent.identifier, TelegramSupervisorBrand, superadminId)
        logger.info { "Superadmin bootstrapped: $displayName (${agent.identifier})" }
    }

    override suspend fun start(scope: CoroutineScope, eventBus: EventBus) {
        val bot = telegramBot(config.token)
        bootstrapSuperadmin(bot)
        pollingJob = bot.buildBehaviourWithLongPolling(scope) {
            commandDispatcher.register(this, bot)
            agentReplyHandler.start(this, bot)
        }
        ticketEventHandler.start(scope, bot, eventBus)
        messageRelayHandler.start(scope, bot, eventBus)
    }

    override suspend fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

}
