plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.channel.api)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktgbotapi)
    implementation(projects.modules.service.channels)
    implementation(projects.modules.service.vault)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
