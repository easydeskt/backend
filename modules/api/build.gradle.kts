plugins {
    id("easydesk-module")
    id("easydesk-ktor-openapi")
}

version = "0.0.1"

dependencies {
    implementation(projects.modules.channel.api)
    implementation(projects.modules.core)
    implementation(projects.modules.service.agents)
    implementation(projects.modules.service.audit)
    implementation(projects.modules.service.channels)
    implementation(projects.modules.service.storage)
    implementation(projects.modules.service.templates)
    implementation(projects.modules.service.tickets)
    implementation(projects.modules.supervisor.api)
    implementation(projects.modules.supervisor.telegram)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.routing.openapi)
}

ktor {
    openApi {
        codeInferenceEnabled = true
        enabled = true
    }
}
