plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(projects.modules.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
