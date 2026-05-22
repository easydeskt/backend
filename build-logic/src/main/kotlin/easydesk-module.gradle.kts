import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

val libs = the<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("io.insert-koin.compiler.plugin")
}

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

base {
    archivesName = project.path
        .removePrefix(":modules:")
        .replace(':', '-')
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.findLibrary("koin-annotations").get())
    implementation(libs.findLibrary("koin-core").get())

    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}