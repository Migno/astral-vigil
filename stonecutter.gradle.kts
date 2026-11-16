plugins {
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("com.diffplug.spotless") version "8.11.0"
}

stonecutter active "26.1-neoforge"

stonecutter parameters {
    swaps["mod_version"] = "\"${properties["mod.version"]}\";"
    swaps["minecraft"] = "\"${current.version}\";"

    replacements {
        // 1.21.11 renamed ResourceLocation -> Identifier. Source of truth is 26.1 (Identifier).
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}

// Spotless needs a repository for the formatter artifacts. It applies the `base` plugin on the
// root project, which Stonecutter warns about; formatting is checked explicitly via `spotlessCheck`.
repositories { mavenCentral() }

spotless {
    isEnforceCheck = false
    java {
        target("src/**/*.java")
        // No code formatter (palantir/google-java-format rewrite `//?` Stonecutter comments and sort
        // imports out of version conditionals). Whitespace hygiene only; style via .editorconfig.
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("misc") {
        target("*.md", "docs/**/*.md", ".gitignore", ".gitattributes", ".editorconfig", "*.toml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
