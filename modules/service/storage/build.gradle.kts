plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.channel.api)
    api(projects.modules.core)

    testImplementation(libs.mockk)
}