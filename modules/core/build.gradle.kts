plugins {
    id("easydesk-module")
}

version = "0.0.1"

dependencies {
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.json)
    api(libs.exposed.kotlin.datetime)
    implementation(libs.flyway.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.jline)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.ktor.server.core)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)
    api(libs.slf4j.api)
    implementation(libs.yamlkt)
}