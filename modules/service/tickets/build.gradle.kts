plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.service.channels)
    api(projects.modules.service.storage)
    api(projects.modules.supervisor.api)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
