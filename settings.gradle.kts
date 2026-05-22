@file:Suppress("UnstableApiUsage")

import java.nio.file.Files
import java.nio.file.Path

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "backend"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val ignoredDirNames = listOf("build", "src")
includeFrom(file(".").toPath(), file("modules").toPath())

fun includeFrom(rootDir: Path, candidateDir: Path) {
    Files.list(candidateDir)
        .filter(Files::isDirectory)
        .forEach { dir ->
            if (Files.isRegularFile(dir.resolve("build.gradle.kts"))) {
                include(":${rootDir.relativize(dir).toString().replace(File.separatorChar, ':')}")
            } else if (!ignoredDirNames.contains(dir.fileName.toString().lowercase())) {
                includeFrom(rootDir, dir)
            }
        }
}