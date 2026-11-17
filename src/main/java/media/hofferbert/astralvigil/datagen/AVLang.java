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

    private static String capital(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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

        add("commands.astralvigil.research.granted", "Granted %s %s knowledge.", "%s %s-Wissen gewährt.");
        add("commands.astralvigil.research.reset", "Research reset.", "Forschung zurückgesetzt.");

        add("commands.astralvigil.starlight.filled", "Stored %s starlight.", "%s Sternenlicht gespeichert.");
        add("commands.astralvigil.starlight.failed", "No starlight device there.", "Dort ist kein Sternenlicht-Gerät.");

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

        // Phase 2 – items
        add("item.astralvigil.star_chart", "Star Chart", "Sternkarte");
        add(
                "item.astralvigil.star_chart.desc",
                "Connect the stars of a revealed constellation to learn it.",
                "Verbinde die Sterne eines enthüllten Sternbilds, um es zu erlernen.");
        add("item.astralvigil.codex_caelestis", "Codex Caelestis", "Codex Caelestis");
        add(
                "item.astralvigil.codex_caelestis.desc",
                "Ilyra's chronicle of the firmament. Grows with your research.",
                "Ilyras Chronik des Firmaments. Wächst mit deiner Forschung.");
        add(
                "message.astralvigil.codex.no_patchouli",
                "The pages are blank. (Install Patchouli to read the Codex.)",
                "Die Seiten sind leer. (Installiere Patchouli, um den Codex zu lesen.)");
        add(
                "astralvigil.codex.landing",
                "The Watchers guard the firmament – and I, Ilyra, keep its chronicle. Read the sky, scan what it touches, and these pages will fill.",
                "Die Wächter hüten das Firmament – und ich, Ilyra, führe seine Chronik. Lies den Himmel, ergründe, was er berührt, und diese Seiten werden sich füllen.");

        // Alignments
        add("alignment.astralvigil.sol", "Sol", "Sol");
        add("alignment.astralvigil.luna", "Luna", "Luna");
        add("alignment.astralvigil.stella", "Stella", "Stella");
        add("alignment.astralvigil.void", "Void", "Leere");
        add("alignment.astralvigil.none", "no alignment", "keine Ausrichtung");
        add("message.astralvigil.scan.new", "Scanned %s: %s", "Ergründet: %s – %s");
        add("message.astralvigil.scan.known", "Already known – %s: %s", "Bereits bekannt – %s: %s");
        add("message.astralvigil.attuned", "You are now attuned to %s.", "Du bist nun auf %s eingestimmt.");
        add(
                "message.astralvigil.constellation.revealed",
                "A new constellation reveals itself on your Star Chart: %s",
                "Ein neues Sternbild zeigt sich auf deiner Sternkarte: %s");
        add(
                "message.astralvigil.constellation.learned",
                "You have learned the constellation %s.",
                "Du hast das Sternbild %s erlernt.");

        // Constellations
        add("constellation.astralvigil.halo", "The Halo", "Der Heiligenschein");
        add("constellation.astralvigil.crescent", "The Crescent", "Die Sichel");
        add("constellation.astralvigil.anvil", "The Anvil", "Der Amboss");
        add("constellation.astralvigil.quill", "The Quill", "Die Feder");

        // Star chart screen
        add("screen.astralvigil.star_chart", "Star Chart", "Sternkarte");
        add("screen.astralvigil.star_chart.clear", "Clear", "Leeren");
        add("screen.astralvigil.star_chart.unknown", "???", "???");
        add(
                "screen.astralvigil.star_chart.pick",
                "Choose a revealed constellation",
                "Wähle ein enthülltes Sternbild");
        add(
                "screen.astralvigil.star_chart.hint",
                "Click a star, then another to draw a line",
                "Klicke einen Stern, dann einen weiteren, um eine Linie zu ziehen");
        add("screen.astralvigil.star_chart.success", "The stars align!", "Die Sterne fügen sich!");
        add("screen.astralvigil.star_chart.learned", "Learned", "Erlernt");

        // Research advancements
        add("advancements.astralvigil.first_scan.title", "First Light", "Erstes Licht");
        add(
                "advancements.astralvigil.first_scan.description",
                "Scan anything with the Astrolabe",
                "Ergründe irgendetwas mit dem Astrolabium");
        for (String[] a : new String[][] {
            {"sol", "Sol", "Sol", "Bask in the sun's domain", "Bade in der Domäne der Sonne"},
            {"luna", "Luna", "Luna", "Walk the moon's quiet paths", "Wandle auf den stillen Pfaden des Mondes"},
            {"stella", "Stella", "Stella", "Trace the starsmith's work", "Folge dem Werk des Sternenschmieds"},
            {"void", "the Void", "die Leere", "Stare into what lies between the stars", "Blicke in das, was zwischen den Sternen liegt"}
        }) {
            add("advancements.astralvigil.attuned_" + a[0] + ".title", "Attuned: " + capital(a[1]), "Eingestimmt: " + capital(a[2]));
            add("advancements.astralvigil.attuned_" + a[0] + ".description", a[3], a[4]);
        }
        add("advancements.astralvigil.constellation_halo.title", "The Halo", "Der Heiligenschein");
        add("advancements.astralvigil.constellation_halo.description", "Learn Solenne's constellation", "Erlerne Solennes Sternbild");
        add("advancements.astralvigil.constellation_crescent.title", "The Crescent", "Die Sichel");
        add("advancements.astralvigil.constellation_crescent.description", "Learn Vael's constellation", "Erlerne Vaels Sternbild");
        add("advancements.astralvigil.constellation_anvil.title", "The Anvil", "Der Amboss");
        add("advancements.astralvigil.constellation_anvil.description", "Learn Orrin's constellation", "Erlerne Orrins Sternbild");
        add("advancements.astralvigil.constellation_quill.title", "The Quill", "Die Feder");
        add("advancements.astralvigil.constellation_quill.description", "Learn Ilyra's constellation", "Erlerne Ilyras Sternbild");

        // Phase 3 – starlight network
        add("block.astralvigil.starlight_collector", "Starlight Collector", "Sternenlichtsammler");
        add("block.astralvigil.starlight_collector.desc", "Drinks starlight under the open night sky.", "Trinkt Sternenlicht unter offenem Nachthimmel.");
        add("block.astralvigil.lens", "Lens", "Linse");
        add("block.astralvigil.lens.desc", "Carries a beam straight on. Use to rotate.", "Führt einen Strahl geradeaus weiter. Benutzen zum Drehen.");
        add("block.astralvigil.mirror", "Mirror", "Spiegel");
        add("block.astralvigil.mirror.desc", "Turns beams from any side towards its face.", "Lenkt Strahlen aus jeder Richtung zu seiner Front.");
        add("block.astralvigil.prism", "Prism", "Prisma");
        add("block.astralvigil.prism.desc", "Splits pure starlight into Sol, Luna and Stella.", "Spaltet reines Sternenlicht in Sol, Luna und Stella.");
        add("block.astralvigil.starlight_reservoir", "Starlight Reservoir", "Sternenlichtspeicher");
        add("block.astralvigil.starlight_reservoir.desc", "Holds starlight of every hue.", "Bewahrt Sternenlicht jeder Färbung.");
        add("tint.astralvigil.pure", "Pure", "Rein");
        add("tint.astralvigil.sol", "Sol", "Sol");
        add("tint.astralvigil.luna", "Luna", "Luna");
        add("tint.astralvigil.stella", "Stella", "Stella");
        add("jade.astralvigil.starlight", "Starlight: %s / %s", "Sternenlicht: %s / %s");
        add("jade.astralvigil.gathering", "Gathering: %s/s", "Sammelt: %s/s");
        add("jade.astralvigil.aim", "Aimed: %s", "Ausrichtung: %s");
        add("config.jade.plugin_astralvigil.starlight", "Astral Vigil Starlight", "Astral Vigil Sternenlicht");
        add("config.jade.plugin_astralvigil.ritual", "Astral Vigil Rituals", "Astral Vigil Rituale");
        add("direction.astralvigil.up", "up", "oben");
        add("direction.astralvigil.down", "down", "unten");
        add("astralvigil.configuration.starlight", "Starlight Network", "Sternenlicht-Netz");
        add("astralvigil.configuration.starlight.collector_rate", "Collector Rate", "Sammelrate");
        add("astralvigil.configuration.starlight.collector_capacity", "Collector Capacity", "Sammler-Kapazität");
        add("astralvigil.configuration.starlight.relay_capacity", "Relay Capacity", "Relais-Kapazität");
        add("astralvigil.configuration.starlight.reservoir_capacity", "Reservoir Capacity", "Speicher-Kapazität");
        add("astralvigil.configuration.starlight.beam_range", "Beam Range", "Strahlreichweite");
        add("astralvigil.configuration.starlight.beam_throughput", "Beam Throughput", "Strahldurchsatz");
        add("astralvigil.configuration.starlight.mirror_loss", "Mirror Loss", "Spiegelverlust");
        add("astralvigil.configuration.starlight.prism_loss", "Prism Loss", "Prismenverlust");

        // Phase 4 – crafting
        add("block.astralvigil.pedestal", "Pedestal", "Podest");
        add("block.astralvigil.pedestal.desc", "Holds one offering for the Convergence Altar.", "Trägt eine Gabe für den Konvergenzaltar.");
        add("block.astralvigil.convergence_altar", "Convergence Altar", "Konvergenzaltar");
        add("block.astralvigil.convergence_altar.desc", "Binds the offerings of its pedestals with starlight.", "Bindet die Gaben seiner Podeste mit Sternenlicht.");
        add("block.astralvigil.astral_forge", "Astral Forge", "Astralschmiede");
        add("block.astralvigil.astral_forge.desc", "Smelts metal with starlight of a single hue.", "Schmilzt Metall mit Sternenlicht einer einzigen Färbung.");
        add("item.astralvigil.starmetal_ingot", "Starmetal Ingot", "Sternenstahlbarren");
        add("item.astralvigil.sunsteel_ingot", "Sunsteel Ingot", "Sonnenstahlbarren");
        add("item.astralvigil.moonsilver_ingot", "Moonsilver Ingot", "Mondsilberbarren");
        add("item.astralvigil.astral_core", "Astral Core", "Astralkern");
        add("item.astralvigil.sigil_blank", "Sigil Blank", "Siegelrohling");
        add("message.astralvigil.altar.busy", "The ritual is underway.", "Das Ritual ist im Gange.");
        add("jade.astralvigil.stability", "Stability: %s%%", "Stabilität: %s%%");
        add("jade.astralvigil.progress", "Progress: %s%%", "Fortschritt: %s%%");
        add("jade.astralvigil.holding", "Holding: %s", "Hält: %s");
        add("recipe.astralvigil.convergence", "Convergence", "Konvergenz");
        add("recipe.astralvigil.astral_forge", "Astral Forge", "Astralschmiede");
        add("recipe.astralvigil.starlight", "%s %s starlight", "%s %s-Sternenlicht");
        add("recipe.astralvigil.time", "%s s", "%s s");
        add("recipe.astralvigil.alignment", "Stabilised by %s", "Stabilisiert durch %s");
        add("emi.category.astralvigil.convergence", "Convergence", "Konvergenz");
        add("emi.category.astralvigil.astral_forge", "Astral Forge", "Astralschmiede");
        add("tag.item.astralvigil.astral_shards", "Astral Shards", "Astralsplitter");
        add("tag.item.astralvigil.chart_paper", "Chart Paper", "Kartenpapier");
        add("tag.item.astralvigil.codex_books", "Codex Books", "Codex-Bücher");
        add("tag.item.astralvigil.meteoric_stones", "Meteoric Stones", "Meteorgesteine");
        add("tag.item.c.gems.astral", "Astral Gems", "Astral-Edelsteine");
        add("tag.item.c.ingots.starmetal", "Starmetal Ingots", "Sternenstahlbarren");
        add("tag.item.c.ingots.sunsteel", "Sunsteel Ingots", "Sonnenstahlbarren");
        add("tag.item.c.ingots.moonsilver", "Moonsilver Ingots", "Mondsilberbarren");
        add("astralvigil.configuration.crafting", "Crafting", "Herstellung");
        add("astralvigil.configuration.crafting.altar_capacity", "Altar Capacity", "Altar-Kapazität");
        add("astralvigil.configuration.crafting.forge_capacity", "Forge Capacity", "Schmiede-Kapazität");
        add("astralvigil.configuration.crafting.altar_radius", "Pedestal Radius", "Podest-Radius");
        add("astralvigil.configuration.crafting.stability_base", "Base Stability", "Grundstabilität");
        add("astralvigil.configuration.crafting.asymmetry_penalty", "Asymmetry Penalty", "Asymmetrie-Abzug");
        add("astralvigil.configuration.crafting.aligned_bonus", "Aligned Block Bonus", "Bonus ausgerichteter Blöcke");
        add("astralvigil.configuration.crafting.aligned_max", "Aligned Bonus Cap", "Obergrenze Ausrichtungsbonus");
        add("astralvigil.configuration.crafting.instability_factor", "Instability Factor", "Instabilitätsfaktor");
        add("astralvigil.configuration.crafting.instability_mobs", "Instability Creatures", "Instabilitäts-Kreaturen");

        // Phase 5 – living gear
        add("item.astralvigil.halo_edge", "Halo Edge", "Halo Edge");
        add("item.astralvigil.dawnbreaker", "Dawnbreaker", "Dawnbreaker");
        add("item.astralvigil.crescent", "Crescent", "Crescent");
        add("item.astralvigil.tidecaller", "Tidecaller", "Tidecaller");
        add("item.astralvigil.starlight_aegis", "Starlight Aegis", "Sternenlicht-Ägis");
        add("item.astralvigil.seraph_helmet", "Seraph Helm", "Seraphenhelm");
        add("item.astralvigil.seraph_chestplate", "Seraph Breastplate", "Seraphenbrustpanzer");
        add("item.astralvigil.seraph_leggings", "Seraph Greaves", "Seraphenbeinschienen");
        add("item.astralvigil.seraph_boots", "Seraph Sabatons", "Seraphenstiefel");
        add("gear.astralvigil.level.0", "Dormant", "Schlummernd");
        add("gear.astralvigil.level.1", "Awakened", "Erwacht");
        add("gear.astralvigil.level.2", "Ascended", "Aufgestiegen");
        add("gear.astralvigil.deed.sword", "Slay %s more undead by daylight", "Erschlage noch %s Untote bei Tageslicht");
        add("gear.astralvigil.deed.axe", "Slay %s more undead by daylight", "Erschlage noch %s Untote bei Tageslicht");
        add("gear.astralvigil.deed.bow", "Slay %s more foes at night (full moon counts thrice)", "Erlege noch %s Feinde bei Nacht (Vollmond zählt dreifach)");
        add("gear.astralvigil.deed.crossbow", "Slay %s more foes at night (full moon counts thrice)", "Erlege noch %s Feinde bei Nacht (Vollmond zählt dreifach)");
        add("gear.astralvigil.deed.shield", "Block %s more attacks", "Wehre noch %s Angriffe ab");
        add("gear.astralvigil.deed.armor", "Slay %s more Void creatures in the full set", "Erschlage noch %s Geschöpfe der Leere in voller Rüstung");
        add("gear.astralvigil.charge", "Charge: %s / %s", "Ladung: %s / %s");
        add("gear.astralvigil.empty_slot", " \u25C7 empty sigil slot", " \u25C7 leerer Siegelplatz");
        add("gear.astralvigil.hint", "%s: ability · %s: stance", "%s: Fähigkeit · %s: Haltung");
        add("gear.astralvigil.sigil_cost", "Costs %s charge · %s s cooldown", "Kostet %s Ladung · %s s Abklingzeit");
        add("message.astralvigil.gear.awakened", "%s is now %s!", "%s ist nun %s!");
        add("message.astralvigil.gear.socketed", "%s set into %s.", "%s in %s eingesetzt.");
        add("message.astralvigil.gear.socket_failed", "The sigil does not fit – wrong gear, already set, or no free slot.", "Das Siegel passt nicht – falsche Ausrüstung, schon eingesetzt oder kein freier Platz.");
        add("message.astralvigil.gear.stance", "Stance: %s", "Haltung: %s");
        add("message.astralvigil.gear.no_stance", "No sigil set in your gear.", "In deiner Ausrüstung ist kein Siegel eingesetzt.");
        add("message.astralvigil.gear.cooldown", "Recovering… %s s", "Erholt sich… %s s");
        add("message.astralvigil.gear.no_charge", "Not enough charge (%s needed).", "Nicht genug Ladung (%s benötigt).");
        add("key.astralvigil.ability", "Gear Ability", "Ausrüstungsfähigkeit");
        add("key.astralvigil.stance", "Cycle Stance", "Haltung wechseln");
        add("key.astralvigil.cosmetics", "Cosmetics", "Kosmetik");

        // Developer cosmetics
        add("screen.astralvigil.cosmetics", "Celestial Wardrobe", "Himmlische Garderobe");
        add("screen.astralvigil.cosmetics.subtitle", "Developer cosmetics - visible to everyone", "Entwickler-Kosmetik - für alle sichtbar");
        add("screen.astralvigil.cosmetics.locked", "Reserved for the Astral Vigil team", "Dem Astral-Vigil-Team vorbehalten");
        add("screen.astralvigil.cosmetics.clear", "Remove All", "Alles ablegen");
        add("screen.astralvigil.cosmetics.turn", "Turn", "Wenden");
        add("screen.astralvigil.cosmetics.spin_on", "Pause", "Anhalten");
        add("screen.astralvigil.cosmetics.spin_off", "Spin", "Drehen");
        add("cosmetic.astralvigil.none", "- none -", "- nichts -");
        add("cosmetic.astralvigil.slot.head", "Head", "Kopf");
        add("cosmetic.astralvigil.slot.back", "Cape", "Umhang");
        add("cosmetic.astralvigil.slot.wings", "Wings", "Flügel");
        add("cosmetic.astralvigil.slot.aura", "Aura", "Aura");
        add("cosmetic.astralvigil.solar_crown", "Solar Crown", "Sonnenkrone");
        add("cosmetic.astralvigil.solar_crown.desc", "Gold, with glowing starlight gems", "Gold mit leuchtenden Sternlichtsteinen");
        add("cosmetic.astralvigil.eclipse_crown", "Eclipse Crown", "Krone der Finsternis");
        add("cosmetic.astralvigil.eclipse_crown.desc", "Jagged spikes that crackle", "Zackige Spitzen, die knistern");
        add("cosmetic.astralvigil.seraph_halo", "Seraph Halo", "Seraphsheiligenschein");
        add("cosmetic.astralvigil.seraph_halo.desc", "A turning ring of living light", "Ein kreisender Ring lebendigen Lichts");
        add("cosmetic.astralvigil.seraph_cape", "Seraph Cape", "Seraphsumhang");
        add("cosmetic.astralvigil.seraph_cape.desc", "White silk with a radiant sun", "Weiße Seide mit strahlender Sonne");
        add("cosmetic.astralvigil.starfield_cape", "Starfield Cape", "Sternenfeldumhang");
        add("cosmetic.astralvigil.starfield_cape.desc", "A night sky with twinkling stars", "Ein Nachthimmel voll funkelnder Sterne");
        add("cosmetic.astralvigil.eclipse_cape", "Eclipse Cape", "Umhang der Finsternis");
        add("cosmetic.astralvigil.eclipse_cape.desc", "A burning violet corona", "Eine glühende violette Korona");
        add("cosmetic.astralvigil.radiant_wings", "Radiant Wings", "Strahlende Flügel");
        add("cosmetic.astralvigil.radiant_wings.desc", "Golden plumes, glowing trim", "Goldene Federn, leuchtender Saum");
        add("cosmetic.astralvigil.eclipse_wings", "Eclipse Wings", "Flügel der Finsternis");
        add("cosmetic.astralvigil.eclipse_wings.desc", "Night-violet, edged in void light", "Nachtviolett, gesäumt von Leerenlicht");
        add("cosmetic.astralvigil.starlight_motes", "Starlight Motes", "Sternlichtfunken");
        add("cosmetic.astralvigil.starlight_motes.desc", "Cyan motes circle you", "Cyanfarbene Funken umkreisen dich");
        add("cosmetic.astralvigil.solar_embers", "Solar Embers", "Sonnenglut");
        add("cosmetic.astralvigil.solar_embers.desc", "Golden embers rise at your feet", "Goldene Glut steigt dir zu Füßen auf");
        add("cosmetic.astralvigil.void_storm", "Void Storm", "Leerensturm");
        add("cosmetic.astralvigil.void_storm.desc", "A violet storm with lightning", "Ein violetter Sturm mit Blitzen");
        add("commands.astralvigil.showcase.built", "Showcase scene '%s' built.", "Schauszene „%s“ errichtet.");
        add("commands.astralvigil.showcase.unknown", "Unknown showcase scene: %s", "Unbekannte Schauszene: %s");
        add("commands.astralvigil.cosmetic.set", "Now wearing %s.", "Du trägst jetzt: %s.");
        add("commands.astralvigil.cosmetic.clear", "All cosmetics removed.", "Alle Kosmetik abgelegt.");
        add("commands.astralvigil.cosmetic.unknown", "Unknown cosmetic: %s", "Unbekannte Kosmetik: %s");
        add("message.astralvigil.cosmetics.locked", "Developer cosmetics are reserved for the Astral Vigil team.", "Entwickler-Kosmetik ist dem Astral-Vigil-Team vorbehalten.");
        add("astralvigil.configuration.cosmetics", "Developer Cosmetics", "Entwickler-Kosmetik");
        add("astralvigil.configuration.cosmetics.enabled", "Enabled", "Aktiviert");
        add("astralvigil.configuration.cosmetics.developers", "Extra Developers (UUIDs)", "Weitere Entwickler (UUIDs)");
        add("key.categories.astralvigil", "Astral Vigil", "Astral Vigil");
        add("key.category.astralvigil.astralvigil", "Astral Vigil", "Astral Vigil");
        String[][] sigils = {
            {"whirl", "Sigil of the Whirl", "Siegel des Wirbels", "Whirl", "Wirbel", "Spin, striking everything around you.", "Wirble herum und triff alles um dich."},
            {"shieldbreaker", "Sigil of the Shieldbreaker", "Siegel des Schildbrechers", "Shieldbreaker", "Schildbrecher", "Your next blow strikes 40% harder.", "Dein nächster Hieb trifft 40 % härter."},
            {"dawn", "Sigil of Dawn", "Siegel der Morgenröte", "Dawn", "Morgenröte", "A burst of daylight: undead burn, the rest are revealed.", "Ein Ausbruch von Tageslicht: Untote brennen, alles andere wird enthüllt."},
            {"flash", "Sigil of the Flash", "Siegel des Lichtblitzes", "Flash", "Lichtblitz", "Dash forward on a streak of light.", "Stürme auf einem Lichtstreif nach vorn."},
            {"precision", "Sigil of Precision", "Siegel der Präzision", "Precision", "Präzision", "Your next arrow strikes far harder and marks its target.", "Dein nächster Pfeil trifft weit härter und markiert sein Ziel."},
            {"undertow", "Sigil of the Undertow", "Siegel des Sogs", "Undertow", "Sog", "Your next shot drags its target towards you.", "Dein nächster Schuss zieht sein Ziel zu dir heran."},
            {"aegis", "Sigil of the Aegis", "Siegel der Ägis", "Aegis", "Ägis", "Wrap yourself in a ward of starlight.", "Hülle dich in einen Schutz aus Sternenlicht."}
        };
        for (String[] s : sigils) {
            add("item.astralvigil.sigil_" + s[0], s[1], s[2]);
            add("sigil.astralvigil." + s[0], s[3], s[4]);
            add("sigil.astralvigil." + s[0] + ".desc", s[5], s[6]);
        }
        add("astralvigil.configuration.gear", "Living Gear", "Lebendige Ausrüstung");
        add("astralvigil.configuration.gear.charge_capacity", "Charge Capacity", "Ladungskapazität");
        add("astralvigil.configuration.gear.charge_rate", "Charge Rate", "Laderate");
        add("astralvigil.configuration.gear.cost_multiplier", "Ability Cost Multiplier", "Multiplikator Fähigkeitskosten");
        add("astralvigil.configuration.gear.cooldown_multiplier", "Ability Cooldown Multiplier", "Multiplikator Abklingzeit");

        // Phase 6 – world
        add("block.astralvigil.star_anchor", "Star Anchor", "Sternenanker");
        add("block.astralvigil.star_anchor.desc", "Lay out a learned constellation, fill with starlight, then touch an anchor.", "Lege ein erlerntes Sternbild aus, fülle es mit Sternenlicht und berühre dann einen Anker.");
        add("block.astralvigil.constellation_lock", "Constellation Lock", "Sternbildschloss");
        add("block.astralvigil.void_crystal", "Void Crystal", "Leerenkristall");
        add("item.astralvigil.void_shard", "Void Shard", "Leerensplitter");
        add("item.astralvigil.lore_fragment", "Lore Fragment", "Chronikfragment");
        add("item.astralvigil.lore_fragment.numbered", "Lore Fragment %s/24", "Chronikfragment %s/24");
        add("message.astralvigil.lock.sealed", "The lock does not yield. It bears the sign of %s.", "Das Schloss gibt nicht nach. Es trägt das Zeichen %s.");
        add("message.astralvigil.lock.opened", "You trace %s – the seal dissolves into light.", "Du ziehst %s nach – das Siegel löst sich in Licht auf.");
        add("message.astralvigil.ritual.no_pattern", "These anchors form no constellation you have learned.", "Diese Anker bilden kein Sternbild, das du erlernt hast.");
        add("message.astralvigil.ritual.no_sky", "An anchor cannot see the sky.", "Ein Anker kann den Himmel nicht sehen.");
        add("message.astralvigil.ritual.no_light", "Every anchor needs %s starlight.", "Jeder Anker braucht %s Sternenlicht.");
        add("message.astralvigil.ritual.resting", "The anchors are still resting.", "Die Anker ruhen noch.");
        add("message.astralvigil.ritual.complete", "%s shines down on you. The Eclipse's gaze recedes.", "%s strahlt auf dich herab. Der Blick der Finsternis weicht zurück.");
        add("message.astralvigil.astrolabe.gaze", "Eclipse Gaze: %s", "Blick der Finsternis: %s");
        add("message.astralvigil.gaze.stage.1", "You feel watched from between the stars.", "Du fühlst dich von zwischen den Sternen beobachtet.");
        add("message.astralvigil.gaze.stage.2", "The dark between the stars has noticed you.", "Die Dunkelheit zwischen den Sternen hat dich bemerkt.");
        add("message.astralvigil.gaze.stage.3", "Where you walk, the world forgets the light.", "Wo du gehst, vergisst die Welt das Licht.");
        add("message.astralvigil.gaze.stage.4", "The Eclipse is looking straight at you.", "Die Finsternis blickt dich unverwandt an.");
        add("message.astralvigil.gaze.whisper.1", "…so bright… so close…", "…so hell… so nah…");
        add("message.astralvigil.gaze.whisper.2", "…the binding is thin here…", "…die Bindung ist hier dünn…");
        add("message.astralvigil.gaze.whisper.3", "…give the light back…", "…gib das Licht zurück…");
        add("message.astralvigil.gaze.whisper.4", "…they abandoned you too…", "…sie haben auch dich verlassen…");
        add("message.astralvigil.gaze.whisper.5", "…look up… look at me…", "…sieh auf… sieh mich an…");
        add("message.astralvigil.gaze.surge", "The sun dims. Something reaches through.", "Die Sonne verblasst. Etwas greift hindurch.");
        add("astralvigil.configuration.gaze", "Eclipse Gaze", "Blick der Finsternis");
        add("astralvigil.configuration.gaze.enabled", "Enabled", "Aktiviert");
        add("astralvigil.configuration.gaze.max", "Maximum", "Maximum");
        add("astralvigil.configuration.gaze.per_ability_cost", "Gaze per Ability Charge", "Blick pro Fähigkeitsladung");
        add("astralvigil.configuration.gaze.per_ritual", "Gaze per Convergence", "Blick pro Konvergenz");
        add("astralvigil.configuration.gaze.crystallize", "Void Crystallisation", "Leerenkristallisierung");
        add("astralvigil.configuration.gaze.event_chance", "Event Chance", "Ereignischance");
        add("astralvigil.configuration.ritual", "Star Anchor Rituals", "Sternenanker-Rituale");
        add("astralvigil.configuration.ritual.starlight", "Starlight per Anchor", "Sternenlicht pro Anker");
        add("astralvigil.configuration.ritual.gaze_relief", "Gaze Relief", "Blick-Linderung");
        add("astralvigil.configuration.ritual.base_seconds", "Blessing Duration (s)", "Segensdauer (s)");
        add("astralvigil.configuration.ritual.cooldown", "Anchor Rest (ticks)", "Ankerruhe (Ticks)");
        for (int i = 0; i < LoreContent.PAGES.size(); i++) {
            LoreContent.Page p = LoreContent.PAGES.get(i);
            add("lore.astralvigil.fragment_" + (i + 1) + ".title", p.titleEn(), p.titleDe());
            add("lore.astralvigil.fragment_" + (i + 1) + ".text", p.en(), p.de());
        }

        // Phase 7 – Firmament
        add("block.astralvigil.firmament_stone", "Firmament Stone", "Firmamentstein");
        add("block.astralvigil.starlight_font", "Starlight Font", "Sternenlichtquell");
        add("block.astralvigil.trial_receptor", "Trial Receptor", "Prüfungsempfänger");
        add("block.astralvigil.trial_goal", "Trial Beacon", "Prüfungsleuchte");
        add("block.astralvigil.moon_target", "Moon Target", "Mondzielscheibe");
        add("item.astralvigil.ascension_key", "Ascension Key", "Aufstiegsschlüssel");
        add("item.astralvigil.ascension_key.desc", "Set it into a finished gate frame. Each kind of gate opens onto a different place in the Firmament.",
                "In einen vollendeten Torrahmen gesetzt. Jede Torart öffnet sich zu einem anderen Ort im Firmament.");
        add("biome.astralvigil.firmament", "Firmament", "Firmament");
        add("block.astralvigil.astral_gate", "Astral Gate", "Astraltor");
        add("block.astralvigil.celestial_portal", "Celestial Portal", "Himmelsportal");
        add("block.astralvigil.solar_gate", "Solar Gate", "Sonnentor");
        add("block.astralvigil.solar_portal", "Solar Portal", "Sonnenportal");
        add("block.astralvigil.lunar_gate", "Lunar Gate", "Mondtor");
        add("block.astralvigil.lunar_portal", "Lunar Portal", "Mondportal");
        add("block.astralvigil.prism_gate", "Prism Gate", "Prismentor");
        add("block.astralvigil.prism_portal", "Prism Portal", "Prismenportal");
        add("block.astralvigil.archive_gate", "Archive Gate", "Archivtor");
        add("block.astralvigil.archive_portal", "Archive Portal", "Archivportal");
        add("block.astralvigil.umbral_gate", "Umbral Gate", "Umbraltor");
        add("block.astralvigil.umbral_portal", "Umbral Portal", "Umbralportal");
        add("message.astralvigil.gate.lit.astral", "The Astral Gate opens onto the Firmament's hub.", "Das Astraltor öffnet sich zur Mitte des Firmaments.");
        add("message.astralvigil.gate.lit.solar", "The Solar Gate opens at the foot of Solenne's Sun Spire.", "Das Sonnentor öffnet sich am Fuß von Solennes Sonnenspitze.");
        add("message.astralvigil.gate.lit.lunar", "The Lunar Gate opens onto Vael's Moon Arena.", "Das Mondtor öffnet sich zu Vaels Mondarena.");
        add("message.astralvigil.gate.lit.prism", "The Prism Gate opens before Orrin's Prism Observatory.", "Das Prismentor öffnet sich vor Orrins Prismenobservatorium.");
        add("message.astralvigil.gate.lit.archive", "The Archive Gate opens before Ilyra's Endless Library.", "Das Archivtor öffnet sich vor Ilyras Endloser Bibliothek.");
        add("message.astralvigil.gate.lit.umbral", "The Umbral Gate tears open. The Throne of the Eclipse lies beyond.", "Das Umbraltor reißt auf. Dahinter liegt der Thron der Finsternis.");
        add("message.astralvigil.gate.umbral_marks", "The Umbral Gate stays dark: carry all four Watchers' Marks.", "Das Umbraltor bleibt dunkel: Trage die Zeichen aller vier Wächter bei dir.");
        add("message.astralvigil.gate.hint", "The key yearns for a gate: build a closed, upright frame of one kind of gate block, then set the key into it.",
                "Der Schlüssel sehnt sich nach einem Tor: Baue einen geschlossenen, aufrechten Rahmen aus einer Sorte Torblöcke und setze den Schlüssel ein.");
        add("message.astralvigil.gate.incomplete", "The frame is not closed (opening at least 2 wide and 3 tall, at most 21).",
                "Der Rahmen ist nicht geschlossen (Öffnung mindestens 2 breit und 3 hoch, höchstens 21).");
        add("dimension.astralvigil.firmament", "The Firmament", "Das Firmament");
        add("message.astralvigil.firmament.arrive", "You rise above the clouds. The gate behind you leads home.", "Du steigst über die Wolken. Das Tor hinter dir führt nach Hause.");
        add("message.astralvigil.firmament.missing", "The Firmament is closed on this server.", "Das Firmament ist auf diesem Server verschlossen.");
        add("message.astralvigil.watcher.favour", "%s regards you. Favour: %s", "%s betrachtet dich. Gunst: %s");
        add("message.astralvigil.trial.complete", "%s accepts your trial and grants you their Mark.", "%s erkennt deine Prüfung an und gewährt dir sein Zeichen.");
        add("message.astralvigil.trial.expired", "Your trial has lapsed. Speak to the Watcher again.", "Deine Prüfung ist verstrichen. Sprich erneut mit dem Wächter.");
        String[][] watchers = {
            {"solenne", "Solenne", "Solenne", "Mark of Solenne", "Zeichen Solennes",
                "%s: \"Climb towards the sun, aspirant. Do not look down.\"", "%s: \"Steig der Sonne entgegen, Anwärter. Sieh nicht hinab.\"",
                "Climb the Sun Spire - a hundred blocks of single steps - and touch the Trial Beacon on its crown. Gold pads are checkpoints; magma pads burn.", "Erklimm die Sonnenspitze - hundert Blöcke einzelner Stufen - und berühre die Prüfungsleuchte auf ihrer Krone. Goldene Stufen sind Rastpunkte, Magmastufen brennen.",
                "Trial of Ascent", "Prüfung des Aufstiegs", "Reach the top of Solenne's sanctum", "Erreiche die Spitze von Solennes Heiligtum"},
            {"vael", "Vael", "Vael", "Mark of Vael", "Zeichen Vaels",
                "%s: \"Ten moons. The shadows will not wait for you.\"", "%s: \"Zehn Monde. Die Schatten werden nicht auf dich warten.\"",
                "Light all ten Moon Targets with your arrows - four float on far islets - while Eclipse Echoes rise against you.", "Entzünde alle zehn Mondzielscheiben mit deinen Pfeilen - vier schweben auf fernen Inselchen - während Finsternis-Echos gegen dich aufsteigen.",
                "Trial of the Hunt", "Prüfung der Jagd", "Strike all ten Moon Targets of Vael's arena", "Triff alle zehn Mondzielscheiben in Vaels Arena"},
            {"orrin", "Orrin", "Orrin", "Mark of Orrin", "Zeichen Orrins",
                "%s: \"Light is metal that has not yet been bent. Bend it.\"", "%s: \"Licht ist Metall, das noch nicht gebogen wurde. Biege es.\"",
                "Three bays, three receptors: turn the mirrors (right-click, sneak to turn back) until Stella light reaches all three Trial Receptors at once.", "Drei Hallen, drei Empfänger: Drehe die Spiegel (Rechtsklick, schleichend zurück), bis Stella-Licht alle drei Prüfungsempfänger zugleich erreicht.",
                "Trial of the Forge", "Prüfung der Schmiede", "Light all three receptors of Orrin's observatory", "Entzünde alle drei Empfänger in Orrins Observatorium"},
            {"ilyra", "Ilyra", "Ilyra", "Mark of Ilyra", "Zeichen Ilyras",
                "%s: \"Every library is a labyrinth. Find the page I hid.\"", "%s: \"Jede Bibliothek ist ein Labyrinth. Finde die Seite, die ich versteckt habe.\"",
                "Find the Trial Beacon in the reading room at the heart of the Endless Library - a dark, roofed maze.", "Finde die Prüfungsleuchte im Lesesaal im Herzen der Endlosen Bibliothek - einem dunklen, überdachten Labyrinth.",
                "Trial of Memory", "Prüfung der Erinnerung", "Find the heart of Ilyra's library", "Finde das Herz von Ilyras Bibliothek"}
        };
        for (String[] w : watchers) {
            add("watcher.astralvigil." + w[0], w[1], w[2]);
            add("entity.astralvigil." + w[0], w[1], w[2]);
            add("item.astralvigil.mark_of_" + w[0], w[3], w[4]);
            add("trial.astralvigil." + w[0] + ".intro", w[5], w[6]);
            add("trial.astralvigil." + w[0] + ".hint", w[7], w[8]);
            add("advancements.astralvigil.trial_" + w[0] + ".title", w[9], w[10]);
            add("advancements.astralvigil.trial_" + w[0] + ".description", w[11], w[12]);
        }
        add("advancements.astralvigil.ascension.title", "Ascension", "Aufstieg");
        add("advancements.astralvigil.ascension.description", "Forge an Ascension Key", "Schmiede einen Aufstiegsschlüssel");

        // Phase 8 – Endgame
        add("entity.astralvigil.eclipse", "The Eclipse", "Die Finsternis");
        add("entity.astralvigil.eclipse_echo", "Eclipse Echo", "Echo der Finsternis");
        add("block.astralvigil.eclipse_seal", "Eclipse Seal", "Siegel der Finsternis");
        add("item.astralvigil.umbral_sigil", "Umbral Sigil", "Umbrales Siegel");
        add("item.astralvigil.umbral_sigil.desc", "In the Firmament, bearing all four Marks: call down the Eclipse.", "Im Firmament, mit allen vier Zeichen: Ruf die Finsternis herab.");
        add("item.astralvigil.eclipse_heart", "Eclipse Heart", "Herz der Finsternis");
        add("item.astralvigil.eclipse_heart.desc", "Still warm with stolen light.", "Noch warm von gestohlenem Licht.");
        add("item.astralvigil.seraph_feather", "Seraph Feather", "Seraphsfeder");
        add("item.astralvigil.seraph_feather.desc", "Use to bind Seraph Wings to yourself; use again to show or hide them.", "Benutzen, um dir Seraphsflügel zu binden; erneut benutzen, um sie zu zeigen oder zu verbergen.");
        add("item.astralvigil.halo_of_dawn", "Halo of Dawn", "Heiligenschein der Morgenröte");
        add("item.astralvigil.halo_of_dawn.desc", "Carried: abilities cost less starlight and the Eclipse's gaze slides off you.", "Getragen: Fähigkeiten kosten weniger Sternenlicht und der Blick der Finsternis gleitet an dir ab.");
        add("message.astralvigil.umbral.not_here", "The sigil is cold. Only on the Umbral Throne - reached through an Umbral Gate - can the Eclipse be called.", "Das Siegel bleibt kalt. Nur auf dem Umbralthron - erreichbar durch ein Umbraltor - lässt sich die Finsternis rufen.");
        add("message.astralvigil.umbral.marks", "The sigil needs all four Watchers' Marks to answer.", "Das Siegel antwortet nur mit den Zeichen aller vier Wächter.");
        add("message.astralvigil.umbral.peaceful", "The Eclipse does not answer in a peaceful world.", "In einer friedlichen Welt antwortet die Finsternis nicht.");
        add("message.astralvigil.umbral.present", "The Eclipse is already here.", "Die Finsternis ist bereits hier.");
        add("message.astralvigil.eclipse.phase2", "The halo splinters – pylons rise to shield it, echoes join the fight!", "Der Heiligenschein zersplittert – Pylonen erheben sich zu ihrem Schutz, Echos greifen ein!");
        add("message.astralvigil.eclipse.phase3", "The sun goes black. Walls rise – use them as cover!", "Die Sonne wird schwarz. Mauern erheben sich – nutze sie als Deckung!");
        add("message.astralvigil.eclipse.nova", "The Eclipse gathers darkness – hide!", "Die Finsternis sammelt Dunkelheit – versteck dich!");
        add("entity.astralvigil.umbral_pylon", "Umbral Pylon", "Umbraler Pylon");
        add("boss.astralvigil.eclipse", "%s – %s", "%s – %s");
        add("boss.astralvigil.eclipse.phase.1", "Fallen Light", "Gefallenes Licht");
        add("boss.astralvigil.eclipse.phase.2", "Shattered Halo", "Zerbrochener Heiligenschein");
        add("boss.astralvigil.eclipse.phase.3", "Total Eclipse", "Totale Finsternis");
        add("boss.astralvigil.eclipse.phase.shield", "Shielded by the Pylons", "Von den Pylonen beschirmt");
        add("boss.astralvigil.eclipse.phase.enraged", "The Last Light", "Das letzte Licht");
        add("attack.astralvigil.eclipse.sweep", "Umbral Sweep", "Umbraler Hieb");
        add("attack.astralvigil.eclipse.lance", "Void Lance", "Leerenlanze");
        add("attack.astralvigil.eclipse.starfall", "Falling Stars", "Fallende Sterne");
        add("attack.astralvigil.eclipse.summon", "Echoes of the Fallen", "Echos der Gefallenen");
        add("attack.astralvigil.eclipse.dive", "Shadow Dive", "Schattensturz");
        add("attack.astralvigil.eclipse.beam", "Eclipse Ray", "Finsternisstrahl");
        add("attack.astralvigil.eclipse.nova", "Umbral Nova", "Umbrale Nova");
        add("attack.astralvigil.eclipse.blink", "Blink", "Schattensprung");
        add("attack.astralvigil.eclipse.grasp", "Umbral Grasp", "Umbraler Griff");
        add("message.astralvigil.eclipse.grasp", "The Eclipse reaches for you through the shadows!", "Die Finsternis greift durch die Schatten nach dir!");
        add("message.astralvigil.eclipse.shield", "The pylons shield the Eclipse – destroy them!", "Die Pylonen schirmen die Finsternis ab – zerstöre sie!");
        add("message.astralvigil.eclipse.shield_broken", "The shield shatters – the Eclipse staggers!", "Der Schild zerbricht – die Finsternis taumelt!");
        add("message.astralvigil.eclipse.grounded", "The Eclipse sinks to the ground – strike now!", "Die Finsternis sinkt zu Boden – schlag jetzt zu!");
        add("message.astralvigil.eclipse.enraged", "It burns with the last of its stolen light.", "Sie brennt mit dem letzten Rest ihres gestohlenen Lichts.");
        add("message.astralvigil.eclipse.peaceful_refund", "The world grows peaceful; the Eclipse withdraws and your Umbral Sigil returns.", "Die Welt wird friedlich; die Finsternis weicht, und dein Umbrales Siegel kehrt zurück.");
        add("message.astralvigil.eclipse.defeated", "The Eclipse falls. For a moment, every star burns brighter.", "Die Finsternis fällt. Für einen Augenblick brennt jeder Stern heller.");
        add("message.astralvigil.wings.shown", "Seraph Wings unfurl.", "Seraphsflügel entfalten sich.");
        add("message.astralvigil.wings.hidden", "Seraph Wings fold away.", "Seraphsflügel falten sich zusammen.");
        add("advancements.astralvigil.umbral_sigil.title", "Umbral Invitation", "Umbrale Einladung");
        add("advancements.astralvigil.umbral_sigil.description", "Forge an Umbral Sigil", "Schmiede ein Umbrales Siegel");
        add("advancements.astralvigil.eclipse_defeated.title", "Dawn After Eclipse", "Morgen nach der Finsternis");
        add("advancements.astralvigil.eclipse_defeated.description", "Defeat the Eclipse", "Besiege die Finsternis");
        add("advancements.astralvigil.halo_of_dawn.title", "Halo Restored", "Der Schein kehrt zurück");
        add("advancements.astralvigil.halo_of_dawn.description", "Forge the Halo of Dawn from the Eclipse Heart", "Schmiede aus dem Herz der Finsternis den Heiligenschein der Morgenröte");
        add("advancements.astralvigil.seraph_wings.title", "Feathers of the Fallen", "Federn der Gefallenen");
        add("advancements.astralvigil.seraph_wings.description", "Obtain a Seraph Feather", "Erhalte eine Seraphsfeder");
        add("astralvigil.configuration.eclipse", "The Eclipse", "Die Finsternis");
        add("astralvigil.configuration.eclipse.lance_damage", "Void Lance Damage", "Schaden der Leerenlanze");
        add("astralvigil.configuration.eclipse.nova_damage", "Umbral Nova Damage", "Schaden der Umbralen Nova");
        add("astralvigil.configuration.eclipse.echoes", "Echoes per Wave", "Echos pro Welle");
        add("astralvigil.configuration.eclipse.feathers", "Seraph Feathers Dropped", "Fallengelassene Seraphsfedern");
        add("astralvigil.configuration.eclipse.health", "Maximum Health", "Maximale Lebenskraft");
        add("astralvigil.configuration.eclipse.healthPerExtraPlayer", "Health per Extra Player", "Lebenskraft je weiterem Spieler");
        add("astralvigil.configuration.eclipse.armor", "Armour", "Rüstung");
        add("astralvigil.configuration.eclipse.sweepDamage", "Umbral Sweep Damage", "Schaden des Umbralen Hiebs");
        add("astralvigil.configuration.eclipse.starDamage", "Falling Star Damage", "Schaden der Fallenden Sterne");
        add("astralvigil.configuration.eclipse.diveDamage", "Shadow Dive Damage", "Schaden des Schattensturzes");
        add("astralvigil.configuration.eclipse.shockwaveDamage", "Shockwave Damage", "Schaden der Druckwelle");
        add("astralvigil.configuration.eclipse.beamDamage", "Eclipse Ray Damage", "Schaden des Finsternisstrahls");
        add("astralvigil.configuration.eclipse.graspDamage", "Umbral Grasp Damage", "Schaden des Umbralen Griffs");
        add("astralvigil.configuration.eclipse.enrageThreshold", "Enrage Threshold", "Schwelle der Raserei");
        add("astralvigil.configuration.eclipse.enrageDamageMultiplier", "Enraged Damage Multiplier", "Schadensfaktor in Raserei");
        add("astralvigil.configuration.eclipse.enrageTempo", "Enraged Tempo", "Tempo in Raserei");
        add("astralvigil.configuration.eclipse.vulnerableDamageMultiplier", "Vulnerable Damage Multiplier", "Schadensfaktor bei Verwundbarkeit");
        add("astralvigil.configuration.eclipse.moveGapTicks", "Pause Between Moves", "Pause zwischen Angriffen");
        add("astralvigil.configuration.eclipse.pylonCount", "Umbral Pylons", "Umbrale Pylonen");
        add("astralvigil.configuration.eclipse.pylonHealth", "Pylon Health", "Lebenskraft eines Pylons");
        add("astralvigil.configuration.eclipse.pylonRadius", "Pylon Radius", "Pylonen-Abstand");
        add("astralvigil.configuration.eclipse.pylonHealPerSecond", "Pylon Healing", "Heilung je Pylon");
        add("astralvigil.configuration.eclipse.coverPillars", "Cover Walls", "Deckungsmauern");
        add("astralvigil.configuration.eclipse.leashRadius", "Leash Radius", "Leinenradius");
        add("astralvigil.configuration.eclipse.resetRange", "Reset Range", "Rücksetzradius");
        add("astralvigil.configuration.eclipse.resetSeconds", "Reset Delay", "Rücksetzverzögerung");
        add("astralvigil.configuration.eclipse.cheeseSeconds", "Hiding Tolerance", "Versteck-Toleranz");
        add("astralvigil.configuration.eclipse.halo_cost", "Halo: Ability Cost Saved", "Heiligenschein: gesparte Fähigkeitskosten");
        add("astralvigil.configuration.eclipse.halo_gaze", "Halo: Gaze Prevented", "Heiligenschein: verhinderter Blick");

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
        add("astralvigil.configuration.research", "Research", "Forschung");
        add("astralvigil.configuration.research.attunement", "Attunement Threshold", "Einstimmungsschwelle");
        add("astralvigil.configuration.research.points_block", "Points per Block", "Punkte pro Block");
        add("astralvigil.configuration.research.points_entity", "Points per Creature", "Punkte pro Kreatur");
        add("astralvigil.configuration.research.points_biome", "Points per Biome", "Punkte pro Biom");
        add("astralvigil.configuration.research.points_structure", "Points per Structure", "Punkte pro Bauwerk");
        add("astralvigil.configuration.research.scan_range", "Scan Range", "Ergründungsreichweite");
        add("astralvigil.configuration.particle_density", "Particle Density", "Partikeldichte");
        add("astralvigil.configuration.screen_shake", "Screen Shake", "Bildschirmwackeln");
        add("astralvigil.configuration.beam_quality", "Beam Quality", "Strahlqualität");
    }
}
