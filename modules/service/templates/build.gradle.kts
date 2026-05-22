plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.service.storage)
    api(projects.modules.supervisor.api)
}
