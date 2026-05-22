plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.channel.api)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.core)
    implementation(projects.modules.service.channels)
}
