pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Project name -> Minecraft version used for version predicates (//? if >=26.1)
        version("1.21.1-neoforge", "1.21.1")
        version("26.1-neoforge", "26.1.2")
        vcsVersion = "26.1-neoforge"
    }
}

rootProject.name = "astral-vigil"
