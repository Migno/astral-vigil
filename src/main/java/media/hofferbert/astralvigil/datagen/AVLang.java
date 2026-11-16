package media.hofferbert.astralvigil.datagen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single source for all translations: every entry carries English <b>and</b> German, so a key can never exist in only
 * one language. Generated into en_us.json / de_de.json by {@link AVLanguageProvider}.
 *
 * <p>German style guide: proper fantasy prose, not word-by-word. Proper names (Solenne, Vael, Orrin, Ilyra, Halo
 * Edge, Dawnbreaker …) stay untranslated.
 */
public final class AVLang {
    public record Entry(String key, String en, String de) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private AVLang() {}

    private static void add(String key, String en, String de) {
        ENTRIES.add(new Entry(key, en, de));
    }

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    static {
        // Creative tab
        add("itemGroup.astralvigil", "Astral Vigil", "Astral Vigil");

        // Items
        add("item.astralvigil.astral_shard", "Astral Shard", "Astralsplitter");
        add(
                "item.astralvigil.astral_shard.desc",
                "A splinter of the firmament, still humming with starlight.",
                "Ein Splitter des Firmaments, noch immer von Sternenlicht durchsummt.");
        add("item.astralvigil.astrolabe", "Astrolabe", "Astrolabium");
        add(
                "item.astralvigil.astrolabe.desc",
                "Reads the heavens. Hums towards fallen stars.",
                "Liest die Himmel. Summt in Richtung gefallener Sterne.");

        // Blocks
        add("block.astralvigil.fallen_star", "Fallen Star", "Gefallener Stern");
        add(
                "block.astralvigil.fallen_star.desc",
                "Its light has not yet learned that it has fallen.",
                "Sein Licht hat noch nicht begriffen, dass es gefallen ist.");
        add("block.astralvigil.meteoric_stone", "Meteoric Stone", "Meteorgestein");

        // Starfall
        add(
                "message.astralvigil.starfall.seen",
                "A star tears through the night sky and falls to the %s!",
                "Ein Stern reißt durch den Nachthimmel und stürzt im %s herab!");
        add("commands.astralvigil.starfall.success", "A star falls towards %s, %s, %s.", "Ein Stern stürzt auf %s, %s, %s herab.");
        add(
                "commands.astralvigil.starfall.failed",
                "No open sky nearby for a star to fall.",
                "Kein offener Himmel in der Nähe, auf den ein Stern fallen könnte.");

        // Directions (used in "falls to the %s" / "im %s")
        add("direction.astralvigil.north", "north", "Norden");
        add("direction.astralvigil.northeast", "northeast", "Nordosten");
        add("direction.astralvigil.east", "east", "Osten");
        add("direction.astralvigil.southeast", "southeast", "Südosten");
        add("direction.astralvigil.south", "south", "Süden");
        add("direction.astralvigil.southwest", "southwest", "Südwesten");
        add("direction.astralvigil.west", "west", "Westen");
        add("direction.astralvigil.northwest", "northwest", "Nordwesten");
        add("direction.astralvigil.here", "right here", "unmittelbaren Umkreis");

        // Astrolabe
        add(
                "message.astralvigil.astrolabe.reading",
                "Starlight %s · Moon: %s · Sky: %s",
                "Sternenlicht %s · Mond: %s · Himmel: %s");
        add(
                "message.astralvigil.astrolabe.starfall",
                "The astrolabe hums: a fallen star lies %s blocks to the %s.",
                "Das Astrolabium summt: Ein gefallener Stern liegt %s Blöcke im %s.");
        add(
                "message.astralvigil.astrolabe.no_starfall",
                "No fallen star resonates nearby.",
                "Kein gefallener Stern schwingt in der Nähe mit.");
        add("sky.astralvigil.open", "open", "offen");
        add("sky.astralvigil.blocked", "hidden", "verborgen");
        add("moon.astralvigil.full_moon", "Full Moon", "Vollmond");
        add("moon.astralvigil.waning_gibbous", "Waning Gibbous", "Abnehmender Dreiviertelmond");
        add("moon.astralvigil.third_quarter", "Last Quarter", "Abnehmender Halbmond");
        add("moon.astralvigil.waning_crescent", "Waning Crescent", "Abnehmende Sichel");
        add("moon.astralvigil.new_moon", "New Moon", "Neumond");
        add("moon.astralvigil.waxing_crescent", "Waxing Crescent", "Zunehmende Sichel");
        add("moon.astralvigil.first_quarter", "First Quarter", "Zunehmender Halbmond");
        add("moon.astralvigil.waxing_gibbous", "Waxing Gibbous", "Zunehmender Dreiviertelmond");

        // Advancements
        add("advancements.astralvigil.root.title", "Astral Vigil", "Astral Vigil");
        add(
                "advancements.astralvigil.root.description",
                "The Watchers guard the firmament. Something has torn it.",
                "Die Wächter hüten das Firmament. Etwas hat es zerrissen.");
        add("advancements.astralvigil.starstruck.title", "Starstruck", "Sternentrunken");
        add(
                "advancements.astralvigil.starstruck.description",
                "Pry an Astral Shard from a Fallen Star",
                "Brich einen Astralsplitter aus einem Gefallenen Stern");
        add("advancements.astralvigil.read_the_heavens.title", "Reading the Heavens", "Die Himmel lesen");
        add(
                "advancements.astralvigil.read_the_heavens.description",
                "Craft an Astrolabe",
                "Fertige ein Astrolabium");

        // Config screen
        add("astralvigil.configuration.title", "Astral Vigil Configuration", "Astral Vigil – Einstellungen");
        add("astralvigil.configuration.section.astralvigil.server.toml", "Server Settings", "Server-Einstellungen");
        add("astralvigil.configuration.section.astralvigil.server.toml.title", "Server Settings", "Server-Einstellungen");
        add("astralvigil.configuration.section.astralvigil.client.toml", "Client Settings", "Client-Einstellungen");
        add("astralvigil.configuration.section.astralvigil.client.toml.title", "Client Settings", "Client-Einstellungen");
        add("astralvigil.configuration.starfall", "Starfall", "Sternenfall");
        add("astralvigil.configuration.starfall.enabled", "Enabled", "Aktiviert");
        add("astralvigil.configuration.starfall.dimensions", "Dimensions", "Dimensionen");
        add("astralvigil.configuration.starfall.check_interval", "Roll Interval (ticks)", "Würfelintervall (Ticks)");
        add("astralvigil.configuration.starfall.chance", "Chance per Roll", "Chance pro Wurf");
        add("astralvigil.configuration.starfall.full_moon_multiplier", "Full Moon Multiplier", "Vollmond-Multiplikator");
        add("astralvigil.configuration.starfall.player_cooldown", "Player Cooldown (ticks)", "Spieler-Abklingzeit (Ticks)");
        add("astralvigil.configuration.starfall.min_distance", "Minimum Distance", "Mindestentfernung");
        add("astralvigil.configuration.starfall.max_distance", "Maximum Distance", "Höchstentfernung");
        add("astralvigil.configuration.starfall.crater_radius", "Crater Radius", "Kraterradius");
        add("astralvigil.configuration.starfall.terrain_damage", "Terrain Damage", "Geländeschaden");
        add("astralvigil.configuration.starfall.announce_radius", "Announcement Radius", "Ankündigungsradius");
        add("astralvigil.configuration.astrolabe", "Astrolabe", "Astrolabium");
        add("astralvigil.configuration.astrolabe.cooldown", "Cooldown (ticks)", "Abklingzeit (Ticks)");
        add("astralvigil.configuration.astrolabe.track_range", "Tracking Range", "Ortungsreichweite");
        add("astralvigil.configuration.particle_density", "Particle Density", "Partikeldichte");
        add("astralvigil.configuration.screen_shake", "Screen Shake", "Bildschirmwackeln");
        add("astralvigil.configuration.beam_quality", "Beam Quality", "Strahlqualität");
    }
}
