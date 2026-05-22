plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    implementation(projects.modules.channel.telegram)
    implementation(projects.modules.core)
    implementation(projects.modules.service.agents)
    implementation(projects.modules.service.channels)
    implementation(projects.modules.service.tickets)
    implementation(projects.modules.supervisor.api)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktgbotapi)
}
