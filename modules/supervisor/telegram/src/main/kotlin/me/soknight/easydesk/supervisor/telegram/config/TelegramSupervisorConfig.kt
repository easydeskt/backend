package me.soknight.easydesk.supervisor.telegram.config

import io.ktor.server.config.*
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
class TelegramSupervisorConfig(@Provided config: ApplicationConfig) {

    private val telegram = config.config("easydesk.supervisor.telegram")

    val superadminId: Long = telegram.property("superadminId").getString().toLong()
    val supergroupId: Long = telegram.property("groupId").getString().toLong()
    val token: String = telegram.property("token").getString()

}
