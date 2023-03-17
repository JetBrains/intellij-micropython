fun config(name: String) = project.findProperty(name).toString()

val ideaVersion = config("ideaVersion")

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.13.2"
}

intellij {
    version.set(ideaVersion)
    pluginName.set("intellij-micropython")
    updateSinceUntilBuild.set(false)
    instrumentCode.set(false)
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
    register<Copy>("copyStubs") {
        dependsOn("prepareSandbox")
        from(projectDir) {
            include("typehints/")
            include("scripts/")
        }
        into("${intellij.sandboxDir.get()}/plugins/intellij-micropython")
    }
    buildPlugin {
        dependsOn("copyStubs")
    }
    runIde {
        dependsOn("copyStubs")
    }
    publishPlugin {
        token.set(config("publishToken"))
    }
}
