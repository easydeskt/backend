plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.channel.api)
    implementation(libs.angus.mail)
    implementation(projects.modules.service.channels)
    implementation(projects.modules.service.vault)

    testImplementation(libs.logback.classic)
    testImplementation(libs.mockk)
}
