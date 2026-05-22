import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    alias(libs.plugins.kotlin.jvm) version libs.versions.kotlin.platform
}

version = "0.0.1"

kotlin {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    }

    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}