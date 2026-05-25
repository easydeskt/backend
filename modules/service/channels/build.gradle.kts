plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.service.agents)
    api(projects.modules.supervisor.api)

    testImplementation(libs.mockk)
}
