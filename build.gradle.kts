fun config(name: String) = project.findProperty(name).toString()

val ideaVersion = config("ideaVersion")

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

intellij {
    version = ideaVersion
    pluginName = "intellij-micropython"
    updateSinceUntilBuild = false
    instrumentCode = false
    plugins.add("terminal")

    if (ideaVersion.contains("PC")) {
        plugins.add("python-ce")
    } else if (ideaVersion.contains("PY")) {
        plugins.add("python")
    } else {
        plugins.add(config("pythonPlugin"))
    }
}

tasks {
    val copyStubs = register<Copy>("copyStubs") {
        dependsOn("prepareSandbox")
        from(projectDir) {
            include("typehints/")
            include("scripts/")
        }
        into("${intellij.sandboxDir.get()}/plugins/intellij-micropython")
    }
    buildSearchableOptions {
        dependsOn(copyStubs)
    }
    buildPlugin {
        dependsOn(copyStubs)
    }
    runIde {
        dependsOn(copyStubs)
    }
    publishPlugin {
        token = config("publishToken")
    }
}
