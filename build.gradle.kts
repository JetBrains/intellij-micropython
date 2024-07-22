import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun config(name: String) = project.findProperty(name).toString()

val ideaVersion = config("ideaVersion")

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            languageVersion = KotlinVersion.DEFAULT
            // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
            apiVersion = KotlinVersion.KOTLIN_1_8
        }
    }
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
    verifyPlugin {
        dependsOn(copyStubs)
    }
    runIde {
        dependsOn(copyStubs)
    }
    publishPlugin {
        token = config("publishToken")
    }
}
