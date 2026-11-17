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
        // GeckoLib 5 (26.1) moved its root package
        string(current.parsed >= "26.1") {
            replace("software.bernie.geckolib", "com.geckolib")
        }
        // 26.2 moved the vanilla entity type constants to EntityTypes and the advancement triggers to .triggers
        string(current.parsed >= "26.2") {
            replace("import net.minecraft.world.entity.EntityType;", "import net.minecraft.world.entity.EntityType; import net.minecraft.world.entity.EntityTypes;")
            replace("net.minecraft.advancements.criterion.", "net.minecraft.advancements.triggers.")
            replace("net.minecraft.advancements.CriteriaTriggers", "net.minecraft.advancements.triggers.CriteriaTriggers")
            replace("getMainCamera()", "mainCamera()")
        }
        // 26.3: SDL input, synced configs, renamed entity/pose helpers
        string(current.parsed >= "26.3") {
            replace("ModConfig.Type.SERVER", "ModConfig.Type.SYNCED")
            replace(".hurtMarked = true", ".syncVelocity = true")
            replace("setInvulnerable(", "setPermanentlyInvulnerable(")
            replace("getStructureManager()", "getStructureTemplateManager()")
            replace("PushReaction.BLOCK)", "PushReaction.IMMOVEABLE)")
            replace("InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_", "InputConstants.Type.KEYBOARD, InputConstants.KEY_")
            replace("import org.lwjgl.glfw.GLFW;", "/* SDL input: no GLFW on 26.3 */")
            replace(".mulPose(Axis.", ".rotate(Axis.")
            replace("state.outlineColor, null);", "state.outlineColor);")
            replace("OverlayTexture.NO_OVERLAY, null, -1, null);", "OverlayTexture.NO_OVERLAY, null, -1, 0);")
            replace("OverlayTexture.NO_OVERLAY, null, color, null);", "OverlayTexture.NO_OVERLAY, null, color, 0);")
        }
        regex(current.parsed >= "26.3") {
            replace("""\.drop\((\w+), false\)""", ".drop($1, false, net.minecraft.util.Prediction.SERVER_ONLY)",
                """\.drop\((\w+), false, net\.minecraft\.util\.Prediction\.SERVER_ONLY\)""", ".drop($1, false)")
        }
        regex(current.parsed >= "26.2") {
            replace("""(?<!\w)EntityType\.([A-Z][A-Z0-9_]+)(?![\w(])""", "EntityTypes.$1", """(?<!\w)EntityTypes\.([A-Z][A-Z0-9_]+)(?![\w(])""", "EntityType.$1")
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
