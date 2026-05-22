@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram.command

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlin.uuid.ExperimentalUuidApi
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.supervisor.api.model.Agent as SupervisorAgent
import me.soknight.easydesk.supervisor.telegram.TelegramSupervisorBrand
import org.koin.core.annotation.Single

@Single
class AddAgentCommand(
    private val agentRepository: AgentRepository,
) {

    suspend fun execute(message: TextMessage, bot: TelegramBot, agent: Agent) {
        val replyTo = message.replyTo
        if (replyTo == null) {
            bot.reply(message, "⚠️ Use /addagent as a reply to a message from the user you want to add.")
            return
        }

        val from = (replyTo as? OptionallyFromUserMessage)?.from
        if (from == null) {
            bot.reply(message, "⚠️ Cannot identify the user from that message.")
            return
        }

        val userId = from.id.chatId.long
        val firstName = from.firstName

        val existing = agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId.toString())
        if (existing != null) {
            bot.reply(message, "⚠️ $firstName is already registered as ${existing.displayName}.")
            return
        }

        val newAgent = agentRepository.create(
            displayName = firstName,
            role = SupervisorAgent.Role.OPERATOR,
            addedByAgentId = agent.identifier,
        )
        agentRepository.linkSupervisor(newAgent.identifier, TelegramSupervisorBrand, userId.toString())
        bot.reply(message, "✅ $firstName added as OPERATOR (agent id: ${newAgent.identifier})")
    }

}
