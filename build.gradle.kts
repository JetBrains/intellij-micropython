import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val pluginName = "intellij-micropython"

plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.intellij.platform") version "2.0.1"
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
        name = pluginName
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
            apiVersion = KotlinVersion.KOTLIN_2_0
        }
    }
    prepareSandbox {
        from("$rootDir") {
            into(pluginName)
            include("typehints/")
            include("scripts/")
        }
    }
}
