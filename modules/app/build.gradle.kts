plugins {
    application
    id("easydesk-module")
}

version = "0.0.1"

application {
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
    mainClass = "me.soknight.easydesk.app.AppKt"
}

dependencies {
    implementation(projects.modules.api)
    implementation(projects.modules.channel.api)
    implementation(projects.modules.channel.email)
    implementation(projects.modules.channel.telegram)
    implementation(projects.modules.channel.vkontakte)
    implementation(projects.modules.service.agents)
    implementation(projects.modules.service.audit)
    implementation(projects.modules.service.channels)
    implementation(projects.modules.service.storage)
    implementation(projects.modules.service.templates)
    implementation(projects.modules.service.tickets)
    implementation(projects.modules.service.vault)
    implementation(projects.modules.supervisor.api)
    implementation(projects.modules.supervisor.telegram)

    implementation(libs.exposed.jdbc)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.kslog.jvm)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.swagger)
    implementation(libs.slf4j.jul.bridge)
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
