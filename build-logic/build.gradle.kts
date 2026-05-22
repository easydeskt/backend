plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.google.ksp.plugin)
    implementation(libs.koin.compiler.plugin)
    implementation(libs.kotlin.jvm.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.ktor.plugin)
}