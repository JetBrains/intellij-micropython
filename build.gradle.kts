import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.0.0"
}

dependencies {
    intellijPlatform {
        val type = project.property("platformType").toString()
        val version = project.property("platformVersion").toString()
        val pythonPlugin = project.property("pythonPlugin").toString()

        create(type, version, useInstaller = false)

        bundledPlugin("org.jetbrains.plugins.terminal")

        when (type) {
            "PC" -> bundledPlugin("PythonCore")
            "PY" -> bundledPlugin("Pythonid")
            else -> plugin(pythonPlugin)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellijPlatform {
    pluginConfiguration {
        name = "intellij-micropython"
    }

    instrumentCode = false

    publishing {
        token = project.property("publishToken").toString()
    }
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            languageVersion = KotlinVersion.DEFAULT
            apiVersion = KotlinVersion.KOTLIN_1_9
        }
    }
    val copyStubs = register<Copy>("copyStubs") {
        dependsOn("prepareSandbox")
        from(projectDir) {
            include("typehints/")
            include("scripts/")
        }
        into("${intellijPlatform.sandboxContainer.get()}/plugins/intellij-micropython")
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
}
