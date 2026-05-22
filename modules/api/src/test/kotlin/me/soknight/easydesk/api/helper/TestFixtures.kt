@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.helper

import io.mockk.every
import io.mockk.mockk
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.buildJsonObject
import me.soknight.easydesk.api.auth.AgentPrincipal
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.channels.data.domain.Channel
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity
import me.soknight.easydesk.service.channels.data.domain.Conversation
import me.soknight.easydesk.service.tickets.data.domain.Ticket
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageStats
import me.soknight.easydesk.service.tickets.data.domain.TicketTag
import me.soknight.easydesk.supervisor.api.model.Agent.Role
import me.soknight.easydesk.supervisor.api.model.Ticket as SupervisorTicket

object TestFixtures {

    val operatorId: Uuid = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val adminId: Uuid = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val now = Clock.System.now()

    val operatorAgent = Agent(
        identifier = operatorId,
        displayName = "Test Operator",
        role = Role.OPERATOR,
        isActive = true,
        createdAt = now,
        updatedAt = now,
    )

    val adminAgent = Agent(
        identifier = adminId,
        displayName = "Test Admin",
        role = Role.ADMIN,
        isActive = true,
        createdAt = now,
        updatedAt = now,
    )

    val operatorPrincipal = AgentPrincipal(operatorAgent, "operator_user")
    val adminPrincipal = AgentPrincipal(adminAgent, "admin_user")

    val testProvider: ChannelProvider = mockk {
        every { brand } returns mockk<ChannelBrand> {
            every { identifier } returns "telegram"
        }
    }

    val channel = Channel(
        id = 1L,
        brand = "telegram",
        displayName = "Test Telegram Channel",
        config = buildJsonObject {},
        attributes = buildJsonObject {},
        isEnabled = true,
        createdAt = now,
        updatedAt = now,
    )

    val identity = ChannelIdentity(
        identifier = 1L,
        channelProvider = testProvider,
        nativeId = "123456789",
        displayName = "Test Client",
        firstSeenAt = now,
        lastSeenAt = now,
    )

    val conversation = Conversation(
        id = 1L,
        channelId = channel.id,
        identityId = identity.identifier,
        attributes = buildJsonObject {},
        createdAt = now,
    )

    val stats = TicketMessageStats(
        attachmentCount = 0,
        lastMessageAt = null,
        previewText = null,
        unreadCount = 0,
    )

    fun ticket(
        id: Long = 1L,
        status: SupervisorTicket.Status = SupervisorTicket.Status.OPEN,
        assignedAgentId: Uuid? = null,
        conversationId: Long = conversation.id,
    ) = Ticket(
        identifier = id,
        conversationId = conversationId,
        status = status,
        priority = SupervisorTicket.Priority.MEDIUM,
        assignedAgentId = assignedAgentId,
        mergedIntoTicketId = null,
        attributes = buildJsonObject {},
        createdAt = now,
        updatedAt = now,
    )

    fun tag(id: Long = 1L, name: String = "Test Tag") = TicketTag(
        identifier = id,
        humanName = name,
        color = null,
        createdAt = now,
    )

}
