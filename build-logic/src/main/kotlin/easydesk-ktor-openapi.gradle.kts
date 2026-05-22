plugins {
    id("io.ktor.plugin")
}

// io.ktor.plugin unconditionally applies the `application` plugin to all Kotlin JVM modules,
// which in turn triggers the Shadow plugin (requiring mainClass). For library modules that
// just need the OpenAPI compiler extension, disable all distribution-related tasks.
afterEvaluate {
    val app = extensions.findByType(JavaApplication::class.java) ?: return@afterEvaluate
    if (app.mainClass.orNull.isNullOrBlank()) {
        app.mainClass.set("_")
        listOf("buildFatJar", "distTar", "distZip", "installDist", "runFatJar", "shadowJar", "startScripts")
            .forEach { tasks.findByName(it)?.enabled = false }
    }
}
