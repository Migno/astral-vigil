import dev.kikugie.stonecutter.controller.task.StonecutterTaskMutexService

plugins {
    id("net.neoforged.moddev")
}

val modId = sc.properties["mod.id"]
val modVersion = sc.properties["mod.version"]
val target = sc.current.project // e.g. "1.21.1-neoforge"
val is26 = sc.current.parsed >= "26.1"
val javaVersion = if (is26) 25 else 21

version = "$modVersion+${sc.current.version}"
group = sc.properties["mod.group"]
base.archivesName = modId

// All dev run directories live in <repo>/run/<target>/<run>
val runRoot = rootProject.layout.projectDirectory.dir("run/$target")

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/", "GeckoLib", "software.bernie.geckolib", "com.geckolib")

    // BlockPilot is only published as GitHub release assets -> ivy pattern repository.
    exclusiveContent {
        forRepository {
            ivy("https://github.com/sparten11740/mc-blockpilot/releases/download") {
                name = "BlockPilot (GitHub releases)"
                patternLayout { artifact("v[revision]/[module]-[revision]+[classifier].[ext]") }
                metadataSources { artifact() }
            }
        }
        filter { includeGroup("dev.blockpilot") }
    }
}

// Mods that only exist in the compat instance (runCompatClient). Never part of the release jar.
val compatMods: Configuration by configurations.creating {
    isTransitive = false
    isCanBeConsumed = false
}

val localRuntime: Configuration by configurations.creating
configurations.runtimeClasspath { extendsFrom(localRuntime) }

dependencies {
    // BlockPilot: AI/scripted in-game testing. Dev runs only (localRuntime is not published).
    localRuntime("dev.blockpilot:blockpilot:${sc.properties["deps.blockpilot"]}:${sc.current.version}-neoforge")

    // Optional integrations: compile against the API, never shipped or required
    compileOnly("maven.modrinth:jade:${sc.properties["deps.jade"]}")
    compileOnly("maven.modrinth:jei:${sc.properties["deps.jei"]}")
    sc.properties["deps.emi"].takeIf { it.isNotEmpty() }?.let { compileOnly("maven.modrinth:emi:$it") }

    sc.properties["compat.mods"].split(',').map(String::trim).filter(String::isNotEmpty).forEach {
        val (slug, ver) = it.split(':', limit = 2)
        compatMods("maven.modrinth:$slug:$ver")
    }

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.google.code.gson:gson:2.11.0") // pure JUnit tests do not see the Minecraft classpath
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

neoForge {
    version = sc.properties["deps.neoforge"]

    if (!is26) {
        parchment {
            minecraftVersion = sc.properties["deps.parchment_mc"]
            mappingsVersion = sc.properties["deps.parchment"]
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.INFO
        }
        register("client") {
            client()
            gameDirectory = runRoot.dir("client").asFile
            // Auto-join the superflat test world only when requested: -Pav.autojoin=true
            if (providers.gradleProperty("av.autojoin").map(String::toBoolean).getOrElse(false)) {
                systemProperty("blockpilot.autojoin", "astralvigil-test")
                systemProperty("blockpilot.autocreate", "flat")
            }
        }
        register("compatClient") {
            client()
            gameDirectory = runRoot.dir("compat").asFile
            if (providers.gradleProperty("av.autojoin").map(String::toBoolean).getOrElse(false)) {
                systemProperty("blockpilot.autojoin", "astralvigil-compat")
                systemProperty("blockpilot.autocreate", "flat")
            }
        }
        register("server") {
            server()
            gameDirectory = runRoot.dir("server").asFile
            programArgument("--nogui")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            gameDirectory = runRoot.dir("gametest").asFile
        }
        register("data") {
            if (is26) clientData() else data()
            gameDirectory = runRoot.dir("data").asFile
            programArguments.addAll(
                "--mod", modId, "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", rootProject.file("src/main/resources/").absolutePath,
            )
        }
    }
}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel", ".cache/**")
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = javaVersion
    }

    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to sc.properties["mod.name"],
            "version" to modVersion,
            "license" to sc.properties["mod.license"],
            "authors" to sc.properties["mod.authors"],
            "minecraft" to sc.properties["mod.mc_compat"],
            "neoforge" to sc.properties["deps.neoforge"],
        )
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
        usesService(StonecutterTaskMutexService.create(gradle, name))
    }

    val prepareCompatMods by registering(Sync::class) {
        group = "astralvigil"
        description = "Copies the compat mods (EMI/JEI, Jade, Curios, Patchouli, Sodium, Iris, Apotheosis, KubeJS) into the compat run dir"
        from(compatMods)
        into(runRoot.dir("compat/mods"))
    }
    matching { it.name == "runCompatClient" }.configureEach { dependsOn(prepareCompatMods) }

    // Dev clients must keep running in the background for BlockPilot: no pause on focus loss, no onboarding.
    val prepareDevOptions by registering {
        group = "astralvigil"
        description = "Writes dev defaults (pauseOnLostFocus:false etc.) into the client run dirs' options.txt"
        val files = listOf(runRoot.file("client/options.txt").asFile, runRoot.file("compat/options.txt").asFile)
        outputs.upToDateWhen { false }
        doLast {
            val wanted = linkedMapOf(
                "pauseOnLostFocus" to "false",
                "onboardAccessibility" to "false",
                "tutorialStep" to "none",
                "skipMultiplayerWarning" to "true",
                "joinedFirstServer" to "true",
            )
            files.forEach { f ->
                f.parentFile.mkdirs()
                val lines = if (f.exists()) f.readLines().toMutableList() else mutableListOf()
                wanted.forEach { (k, v) ->
                    val i = lines.indexOfFirst { it.startsWith("$k:") }
                    if (i >= 0) lines[i] = "$k:$v" else lines.add("$k:$v")
                }
                f.writeText(lines.joinToString("\n", postfix = "\n"))
            }
        }
    }
    matching { it.name == "runClient" || it.name == "runCompatClient" }.configureEach { dependsOn(prepareDevOptions) }

    test {
        useJUnitPlatform()
        systemProperty("av.generatedResources", file("src/generated/resources").absolutePath)
        systemProperty("av.modId", modId)
    }

    withType<Jar>().configureEach {
        val suffix = "_" + modId // local copy: lambdas must not capture the script object
        from(rootProject.file("LICENSE")) { rename { it + suffix } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to <root>/build/libs/<mod version>/"
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    }
}
