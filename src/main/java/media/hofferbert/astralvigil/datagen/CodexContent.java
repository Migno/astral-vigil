package media.hofferbert.astralvigil.datagen;

import com.google.gson.JsonObject;
import java.util.List;

/**
 * Text of the Codex Caelestis in English and German. Written as Ilyra's chronicle; German is prose, not a literal
 * translation. Patchouli markup: $(item)…$() highlights, $(br) line break, $(l)…$() bold.
 */
public final class CodexContent {
    private CodexContent() {}

    public record Category(String id, String nameEn, String nameDe, String descEn, String descDe, String icon, int sort) {}

    public record Entry(
            String id, String category, String nameEn, String nameDe, String icon, int sort, String advancement, List<Page> pages) {}

    public interface Page {
        JsonObject toJson(boolean german);
    }

    public record Text(String en, String de) implements Page {
        @Override
        public JsonObject toJson(boolean german) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "patchouli:text");
            o.addProperty("text", german ? de : en);
            return o;
        }
    }

    public record Spotlight(String item, String en, String de) implements Page {
        @Override
        public JsonObject toJson(boolean german) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "patchouli:spotlight");
            o.addProperty("item", item);
            o.addProperty("text", german ? de : en);
            return o;
        }
    }

    public record Crafting(String recipe, String en, String de) implements Page {
        @Override
        public JsonObject toJson(boolean german) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "patchouli:crafting");
            o.addProperty("recipe", recipe);
            o.addProperty("text", german ? de : en);
            return o;
        }
    }

    public static final List<Category> CATEGORIES = List.of(
            new Category(
                    "the_rift",
                    "The Rift",
                    "Der Riss",
                    "Something has torn the firmament. Stars are falling.",
                    "Etwas hat das Firmament zerrissen. Sterne fallen.",
                    "astralvigil:fallen_star",
                    0),
            new Category(
                    "instruments",
                    "Instruments",
                    "Instrumente",
                    "Tools for reading the heavens.",
                    "Werkzeuge, um die Himmel zu lesen.",
                    "astralvigil:astrolabe",
                    1),
            new Category(
                    "alignments",
                    "Celestial Alignments",
                    "Himmlische Ausrichtungen",
                    "Everything beneath the sky leans towards one of four lights – or towards the dark between them.",
                    "Alles unter dem Himmel neigt sich einem von vier Lichtern zu – oder der Dunkelheit dazwischen.",
                    "astralvigil:codex_caelestis",
                    2),
            new Category(
                    "starlight",
                    "Starlight",
                    "Sternenlicht",
                    "Gather the light of the night sky and bend it to your will.",
                    "Sammle das Licht des Nachthimmels und beuge es deinem Willen.",
                    "astralvigil:starlight_collector",
                    2),
            new Category(
                    "convergence",
                    "Convergence",
                    "Konvergenz",
                    "Where starlight binds matter together.",
                    "Wo Sternenlicht Materie aneinanderbindet.",
                    "astralvigil:convergence_altar",
                    3),
            new Category(
                    "living_gear",
                    "Living Gear",
                    "Lebendige Ausrüstung",
                    "Weapons and armour that remember what you did with them.",
                    "Waffen und Rüstungen, die sich erinnern, was du mit ihnen getan hast.",
                    "astralvigil:halo_edge",
                    4),
            new Category(
                    "the_eclipse",
                    "The Eclipse",
                    "Die Finsternis",
                    "The fifth light, and how to keep its gaze away.",
                    "Das fünfte Licht, und wie man seinen Blick fernhält.",
                    "astralvigil:void_crystal",
                    5),
            new Category(
                    "firmament",
                    "The Firmament",
                    "Das Firmament",
                    "Above the clouds, the Watchers wait.",
                    "Über den Wolken warten die Wächter.",
                    "astralvigil:ascension_key",
                    6),
            new Category(
                    "chronicle",
                    "The Lost Chronicle",
                    "Die verlorene Chronik",
                    "Pages of my old chronicle, scattered among the fallen observatories.",
                    "Seiten meiner alten Chronik, verstreut in den gefallenen Observatorien.",
                    "astralvigil:lore_fragment",
                    7),
            new Category(
                    "constellations",
                    "Constellations",
                    "Sternbilder",
                    "The Watchers' signs, written in stars.",
                    "Die Zeichen der Wächter, in Sterne geschrieben.",
                    "astralvigil:star_chart",
                    6));


    public static final List<Entry> ENTRIES;

    private static final List<Entry> ENTRIES_BASE = List.of(
            // ---------------------------------------------------------------- the rift
            new Entry("watchers", "the_rift", "The Watchers", "Die Wächter", "astralvigil:astral_shard", 0, null, List.of(
                    new Text(
                            "Four Watchers keep the firmament: $(l)Solenne$() of the sun, $(l)Vael$() of the moon, $(l)Orrin$() the starsmith, and I, $(l)Ilyra$(), who writes it all down.$(br2)There was a fifth. We do not speak the name. We call it the $(l)Eclipse$().",
                            "Vier Wächter hüten das Firmament: $(l)Solenne$() der Sonne, $(l)Vael$() des Mondes, $(l)Orrin$() der Sternenschmied, und ich, $(l)Ilyra$(), die alles niederschreibt.$(br2)Es gab einen Fünften. Seinen Namen sprechen wir nicht aus. Wir nennen ihn die $(l)Finsternis$()."),
                    new Text(
                            "A rift has opened in the sky. Through it, fragments of the firmament fall to your world – and through it, the Eclipse stirs.$(br2)You have looked up. That makes you a $(l)Seeker$(). Whether you become more is up to you.",
                            "Ein Riss hat sich im Himmel aufgetan. Durch ihn fallen Splitter des Firmaments auf eure Welt – und durch ihn regt sich die Finsternis.$(br2)Du hast aufgeblickt. Das macht dich zum $(l)Suchenden$(). Ob du mehr wirst, liegt bei dir."))),
            new Entry("starfall", "the_rift", "Starfall", "Sternenfall", "astralvigil:fallen_star", 1, null, List.of(
                    new Text(
                            "On clear nights, stars fall. You will see the streak across the sky and hear the impact; the chronicle notes in which direction it came down.$(br2)Under a $(l)full moon$() the sky sheds more often.",
                            "In klaren Nächten fallen Sterne. Du siehst den Streif am Himmel und hörst den Einschlag; die Chronik vermerkt, in welcher Richtung er niederging.$(br2)Bei $(l)Vollmond$() verliert der Himmel häufiger Sterne."),
                    new Spotlight(
                            "astralvigil:fallen_star",
                            "Each impact leaves a small crater lined with $(item)Meteoric Stone$() and, at its heart, a $(item)Fallen Star$(). Break it with an iron pickaxe to pry loose $(item)Astral Shards$().",
                            "Jeder Einschlag hinterlässt einen kleinen Krater, ausgekleidet mit $(item)Meteorgestein$(), und in seinem Herzen einen $(item)Gefallenen Stern$(). Brich ihn mit einer Eisenspitzhacke, um $(item)Astralsplitter$() zu lösen."),
                    new Spotlight(
                            "astralvigil:astral_shard",
                            "Astral Shards still hum with starlight. They are the root of everything in this chronicle.",
                            "Astralsplitter summen noch immer vor Sternenlicht. Aus ihnen erwächst alles in dieser Chronik."))),
            // ---------------------------------------------------------------- instruments
            new Entry("astrolabe", "instruments", "The Astrolabe", "Das Astrolabium", "astralvigil:astrolabe", 0, null, List.of(
                    new Crafting(
                            "astralvigil:astrolabe",
                            "Your first instrument.",
                            "Dein erstes Instrument."),
                    new Text(
                            "$(l)Use$() it towards the sky to read starlight, the moon's phase and whether the heavens can see you. It also hums towards recently fallen stars.$(br2)$(l)Use it on a block or creature$() to scan its celestial alignment. Used in the open, it reads the biome – or the structure you stand in.",
                            "$(l)Benutze$() es gen Himmel, um Sternenlicht, Mondphase und freie Sicht zu lesen. Es summt zudem in Richtung kürzlich gefallener Sterne.$(br2)$(l)Benutze es an einem Block oder Geschöpf$(), um dessen himmlische Ausrichtung zu ergründen. Unter freiem Himmel liest es das Biom – oder das Bauwerk, in dem du stehst."))),
            new Entry("star_chart", "instruments", "The Star Chart", "Die Sternkarte", "astralvigil:star_chart", 1, null, List.of(
                    new Crafting(
                            "astralvigil:star_chart",
                            "Vellum that remembers the sky.",
                            "Pergament, das sich an den Himmel erinnert."),
                    new Text(
                            "Once you are attuned to an alignment, its constellation reveals itself on the chart. Click a star, then another to draw a line; click a line again to erase it.$(br2)Beware the stray stars – not every light belongs to the sign.",
                            "Sobald du auf eine Ausrichtung eingestimmt bist, zeigt sich ihr Sternbild auf der Karte. Klicke einen Stern, dann einen weiteren, um eine Linie zu ziehen; ein erneuter Klick löscht sie.$(br2)Hüte dich vor verirrten Sternen – nicht jedes Licht gehört zum Zeichen."))),
            new Entry("codex", "instruments", "Codex Caelestis", "Codex Caelestis", "astralvigil:codex_caelestis", 2, null, List.of(
                    new Crafting(
                            "astralvigil:codex_caelestis",
                            "You are reading it.",
                            "Du liest ihn gerade."))),
            // ---------------------------------------------------------------- alignments
            new Entry("alignments", "alignments", "The Four Lights", "Die vier Lichter", "astralvigil:codex_caelestis", 0, null, List.of(
                    new Text(
                            "Every stone, creature, land and ruin leans towards an alignment: $(l)Sol$(), $(l)Luna$(), $(l)Stella$() – or the $(l)Void$().$(br2)Scanning something new grants knowledge of its alignment. Gather enough and you become $(l)attuned$(): new pages and constellations open to you.",
                            "Jeder Stein, jedes Geschöpf, jedes Land und jede Ruine neigt sich einer Ausrichtung zu: $(l)Sol$(), $(l)Luna$(), $(l)Stella$() – oder der $(l)Leere$().$(br2)Etwas Neues zu ergründen schenkt Wissen über seine Ausrichtung. Sammle genug, und du wirst $(l)eingestimmt$(): Neue Seiten und Sternbilder öffnen sich dir."))),
            new Entry("sol", "alignments", "Sol", "Sol", "minecraft:sunflower", 1, "research/attuned_sol", List.of(
                    new Text(
                            "$(l)Solenne's light.$() Heat, gold and honey; deserts, savannas and the burning lands below. Sol favours those who fight by day and lay the restless dead to rest.",
                            "$(l)Solennes Licht.$() Hitze, Gold und Honig; Wüsten, Savannen und die brennenden Lande in der Tiefe. Sol begünstigt jene, die bei Tag kämpfen und die ruhelosen Toten zur Ruhe betten."))),
            new Entry("luna", "alignments", "Luna", "Luna", "minecraft:snow_block", 2, "research/attuned_luna", List.of(
                    new Text(
                            "$(l)Vael's light.$() Snow, ice and still water; wolves and foxes that hunt beneath her. Luna rewards patience, the night hunt and the single precise strike.",
                            "$(l)Vaels Licht.$() Schnee, Eis und stilles Wasser; Wölfe und Füchse, die unter ihr jagen. Luna belohnt Geduld, die nächtliche Jagd und den einen, präzisen Treffer."))),
            new Entry("stella", "alignments", "Stella", "Stella", "minecraft:amethyst_cluster", 3, "research/attuned_stella", List.of(
                    new Text(
                            "$(l)Orrin's forge-light.$() Amethyst, end stone and all that fell from above; high peaks closest to the sky. Stella belongs to builders, smiths and those who arrange the stars themselves.",
                            "$(l)Orrins Schmiedelicht.$() Amethyst, Endstein und alles, was von oben fiel; hohe Gipfel, dem Himmel am nächsten. Stella gehört den Baumeistern, Schmieden und jenen, die die Sterne selbst ordnen."))),
            new Entry("void", "alignments", "The Void", "Die Leere", "minecraft:crying_obsidian", 4, "research/attuned_void", List.of(
                    new Text(
                            "$(l)Not a light.$() The dark between the stars: sculk, obsidian, the deep places and the restless dead. The Void listens. Study it – but remember who else is listening.",
                            "$(l)Kein Licht.$() Die Dunkelheit zwischen den Sternen: Sculk, Obsidian, die tiefen Orte und die ruhelosen Toten. Die Leere lauscht. Erforsche sie – doch vergiss nicht, wer sonst noch lauscht."))),
            // ---------------------------------------------------------------- starlight
            new Entry("collector", "starlight", "Starlight Collector", "Sternenlichtsammler", "astralvigil:starlight_collector", 0, null, List.of(
                    new Text(
                            "Starlight only falls under the open sky – strongest at midnight, under a full moon, on a clear night. A $(item)Starlight Collector$() drinks it in and sends it out of its front as a visible beam.$(br2)Rain halves the light; storms nearly drown it.",
                            "Sternenlicht fällt nur unter offenem Himmel – am stärksten um Mitternacht, bei Vollmond, in klarer Nacht. Ein $(item)Sternenlichtsammler$() trinkt es und sendet es als sichtbaren Strahl aus seiner Front.$(br2)Regen halbiert das Licht; Gewitter ertränken es fast."),
                    new Crafting("astralvigil:starlight_collector", "", ""))),
            new Entry("beams", "starlight", "Lenses and Mirrors", "Linsen und Spiegel", "astralvigil:lens", 1, null, List.of(
                    new Text(
                            "Beams travel up to sixteen blocks and pass through glass. A $(item)Lens$() carries a beam straight on without loss; a $(item)Mirror$() catches beams from any side and turns them towards its face, losing a little light.$(br2)Use either with an empty hand to rotate it; sneak to rotate back.",
                            "Strahlen reichen bis zu sechzehn Blöcke weit und durchdringen Glas. Eine $(item)Linse$() führt einen Strahl verlustfrei geradeaus weiter; ein $(item)Spiegel$() fängt Strahlen aus jeder Richtung und lenkt sie zu seiner Front – mit ein wenig Verlust.$(br2)Mit leerer Hand benutzen, um sie zu drehen; schleichend dreht man zurück."),
                    new Crafting("astralvigil:lens", "", ""),
                    new Crafting("astralvigil:mirror", "", ""))),
            new Entry("prism", "starlight", "The Prism", "Das Prisma", "astralvigil:prism", 2, "research/first_scan", List.of(
                    new Text(
                            "Pure starlight is all lights at once. A $(item)Prism$() parts it: $(l)Stella$() leaves its front, $(l)Sol$() its left and $(l)Luna$() its right side. Some light is lost in the parting.$(br2)Many rituals will ask for one hue alone.",
                            "Reines Sternenlicht ist alle Lichter zugleich. Ein $(item)Prisma$() scheidet es: $(l)Stella$() verlässt seine Front, $(l)Sol$() die linke und $(l)Luna$() die rechte Seite. Beim Scheiden geht etwas Licht verloren.$(br2)Viele Rituale verlangen nach einer einzigen Färbung."),
                    new Crafting("astralvigil:prism", "", ""))),
            new Entry("reservoir", "starlight", "Starlight Reservoir", "Sternenlichtspeicher", "astralvigil:starlight_reservoir", 3, null, List.of(
                    new Text(
                            "A $(item)Starlight Reservoir$() keeps every hue apart and safe until it is needed. A comparator reads how full it is.",
                            "Ein $(item)Sternenlichtspeicher$() hält jede Färbung getrennt und sicher, bis sie gebraucht wird. Ein Komparator liest seinen Füllstand."),
                    new Crafting("astralvigil:starlight_reservoir", "", ""))),
            // ---------------------------------------------------------------- convergence
            new Entry("astral_forge", "convergence", "The Astral Forge", "Die Astralschmiede", "astralvigil:astral_forge", 0, null, List.of(
                    new Text(
                            "Place up to four items into the $(item)Astral Forge$() one at a time; take them back with an empty hand. Feed it starlight of the right hue – a beam, or a $(item)Starlight Reservoir$() touching it – and it forges on its own.$(br2)$(l)Sunsteel$() wants Sol, $(l)Moonsilver$() Luna, $(l)Starmetal$() Stella light.",
                            "Lege bis zu vier Gegenstände einzeln in die $(item)Astralschmiede$(); mit leerer Hand nimmst du sie zurück. Speise sie mit Sternenlicht der richtigen Färbung – über einen Strahl oder einen berührenden $(item)Sternenlichtspeicher$() – und sie schmiedet von selbst.$(br2)$(l)Sonnenstahl$() verlangt Sol-, $(l)Mondsilber$() Luna-, $(l)Sternenstahl$() Stella-Licht."),
                    new Crafting("astralvigil:astral_forge", "", ""))),
            new Entry("convergence_altar", "convergence", "The Convergence Altar", "Der Konvergenzaltar", "astralvigil:convergence_altar", 1, null, List.of(
                    new Text(
                            "Set the catalyst on the $(item)Convergence Altar$() and the offerings on $(item)Pedestals$() within four blocks. Fill the altar with starlight, then touch it with an empty hand to begin. Sneak to take the result.",
                            "Lege den Katalysator auf den $(item)Konvergenzaltar$() und die Gaben auf $(item)Podeste$() im Umkreis von vier Blöcken. Fülle den Altar mit Sternenlicht und berühre ihn dann mit leerer Hand, um zu beginnen. Schleichend nimmst du das Ergebnis."),
                    new Text(
                            "$(l)Stability$() decides whether the ritual holds. Every pedestal needs a mirrored partner across the altar. Blocks of the recipe's alignment nearby steady it; Void blocks and a hidden sky unsettle it.$(br2)An unstable ritual may fling an offering away – or open a door for something hungry.",
                            "$(l)Stabilität$() entscheidet, ob das Ritual hält. Jedes Podest braucht einen gespiegelten Partner jenseits des Altars. Blöcke der Ausrichtung des Rezepts stützen es; Blöcke der Leere und ein verborgener Himmel bringen es ins Wanken.$(br2)Ein instabiles Ritual kann eine Gabe fortschleudern – oder eine Tür öffnen für etwas Hungriges."),
                    new Crafting("astralvigil:convergence_altar", "", ""),
                    new Crafting("astralvigil:pedestal", "", ""))),
            new Entry("astral_core", "convergence", "Astral Core", "Astralkern", "astralvigil:astral_core", 2, null, List.of(
                    new Spotlight(
                            "astralvigil:astral_core",
                            "Catalyst: a diamond. Offerings: one ingot each of Starmetal, Sunsteel and Moonsilver, and an Astral Shard. 1000 pure starlight. Stella blocks steady it.",
                            "Katalysator: ein Diamant. Gaben: je ein Barren Sternenstahl, Sonnenstahl und Mondsilber sowie ein Astralsplitter. 1000 reines Sternenlicht. Stella-Blöcke stützen es."),
                    new Spotlight(
                            "astralvigil:sigil_blank",
                            "Catalyst: a Starmetal Ingot. Offerings: two gold nuggets and two amethyst shards. 400 Stella starlight. Yields two blanks – the Watchers' sigils will be cut from them.",
                            "Katalysator: ein Sternenstahlbarren. Gaben: zwei Goldklumpen und zwei Amethystsplitter. 400 Stella-Sternenlicht. Ergibt zwei Rohlinge – aus ihnen werden die Siegel der Wächter geschnitten."))),
            // ---------------------------------------------------------------- living gear
            new Entry("living_gear", "living_gear", "Living Gear", "Lebendige Ausrüstung", "astralvigil:halo_edge", 0, null, List.of(
                    new Text(
                            "Gear forged at the $(item)Convergence Altar$() is $(l)dormant$(). Deeds in the spirit of its Watcher awaken it: undead slain by daylight for Solenne's blades, night kills for Vael's bows, blocked blows for the Aegis, Void creatures for the Seraph Plate.$(br2)$(l)Awakened$() gear gains a second sigil slot, $(l)Ascended$() gear a third.",
                            "Am $(item)Konvergenzaltar$() geschmiedete Ausrüstung $(l)schlummert$(). Taten im Geist ihres Wächters wecken sie: bei Tageslicht erschlagene Untote für Solennes Klingen, nächtliche Jagd für Vaels Bögen, abgewehrte Hiebe für die Ägis, Geschöpfe der Leere für die Seraphenrüstung.$(br2)$(l)Erwachte$() Ausrüstung erhält einen zweiten Siegelplatz, $(l)aufgestiegene$() einen dritten."),
                    new Text(
                            "Hold the gear, take a $(item)Sigil$() in your off hand and use while sneaking to set it. Each sigil is a $(l)stance$(): switch with the stance key, unleash with the ability key.$(br2)Abilities cost $(l)charge$(). Gear recharges under the night sky – or touch a $(item)Starlight Reservoir$() with it.",
                            "Halte die Ausrüstung, nimm ein $(item)Siegel$() in die Nebenhand und benutze sie schleichend, um es einzusetzen. Jedes Siegel ist eine $(l)Haltung$(): wechsle sie mit der Haltungstaste, entfessle sie mit der Fähigkeitstaste.$(br2)Fähigkeiten kosten $(l)Ladung$(). Ausrüstung lädt sich unter dem Nachthimmel auf – oder berühre damit einen $(item)Sternenlichtspeicher$()."))),
            new Entry("solenne_gear", "living_gear", "Solenne's Blades", "Solennes Klingen", "astralvigil:dawnbreaker", 1, null, List.of(
                    new Spotlight("astralvigil:halo_edge",
                            "A Sunsteel sword. Catalyst: a diamond sword; offerings: two Sunsteel ingots and two Astral Shards; 800 Sol light.",
                            "Ein Sonnenstahlschwert. Katalysator: ein Diamantschwert; Gaben: zwei Sonnenstahlbarren und zwei Astralsplitter; 800 Sol-Licht."),
                    new Spotlight("astralvigil:dawnbreaker",
                            "A Sunsteel battle axe. Catalyst: a diamond axe; offerings: three Sunsteel ingots and an Astral Shard; 900 Sol light.",
                            "Eine Sonnenstahl-Streitaxt. Katalysator: eine Diamantaxt; Gaben: drei Sonnenstahlbarren und ein Astralsplitter; 900 Sol-Licht."))),
            new Entry("vael_gear", "living_gear", "Vael's Bows", "Vaels Bögen", "astralvigil:crescent", 2, null, List.of(
                    new Spotlight("astralvigil:crescent",
                            "A Moonsilver bow whose arrows bite deeper at night. Catalyst: a bow; offerings: two Moonsilver ingots and two strings; 800 Luna light.",
                            "Ein Mondsilberbogen, dessen Pfeile nachts tiefer beißen. Katalysator: ein Bogen; Gaben: zwei Mondsilberbarren und zwei Fäden; 800 Luna-Licht."),
                    new Spotlight("astralvigil:tidecaller",
                            "A Moonsilver crossbow. Catalyst: a crossbow; offerings: two Moonsilver ingots and two prismarine crystals; 900 Luna light.",
                            "Eine Mondsilber-Armbrust. Katalysator: eine Armbrust; Gaben: zwei Mondsilberbarren und zwei Prismarinkristalle; 900 Luna-Licht."))),
            new Entry("orrin_gear", "living_gear", "Orrin's Plate", "Orrins Rüstung", "astralvigil:seraph_chestplate", 3, null, List.of(
                    new Spotlight("astralvigil:seraph_chestplate",
                            "Seraph Plate, forged from diamond armour and Starmetal under Stella light. Awakened, the full set lets you glide while sneaking; ascended, you no longer fear falls.",
                            "Seraphenrüstung, aus Diamantrüstung und Sternenstahl unter Stella-Licht geschmiedet. Erwacht lässt dich der volle Satz schleichend gleiten; aufgestiegen fürchtest du keinen Sturz mehr."),
                    new Spotlight("astralvigil:starlight_aegis",
                            "The Starlight Aegis, a Starmetal shield. Every blocked blow brings it closer to waking.",
                            "Die Sternenlicht-Ägis, ein Schild aus Sternenstahl. Jeder abgewehrte Hieb bringt sie dem Erwachen näher."))),
            new Entry("sigils", "living_gear", "Sigils", "Siegel", "astralvigil:sigil_flash", 4, null, List.of(
                    new Text(
                            "Sigils are cut from $(item)Sigil Blanks$() at the altar. $(l)Whirl$() and $(l)Shieldbreaker$() suit the axe, $(l)Dawn$() and $(l)Flash$() the sword, $(l)Precision$() and $(l)Undertow$() the bows, $(l)Aegis$() the shield and the Seraph Plate.",
                            "Siegel werden am Altar aus $(item)Siegelrohlingen$() geschnitten. $(l)Wirbel$() und $(l)Schildbrecher$() passen zur Axt, $(l)Morgenröte$() und $(l)Lichtblitz$() zum Schwert, $(l)Präzision$() und $(l)Sog$() zu den Bögen, $(l)Ägis$() zu Schild und Seraphenrüstung."))),
            // ---------------------------------------------------------------- the eclipse
            new Entry("gaze", "the_eclipse", "The Eclipse's Gaze", "Der Blick der Finsternis", "astralvigil:void_shard", 0, null, List.of(
                    new Text(
                            "Every ability you unleash and every convergence you perform draws the $(l)Eclipse's Gaze$(). Your Astrolabe shows how strongly it watches.$(br2)At first it only whispers. Then it sends $(l)Echoes$(), crystallises the ground around you into $(item)Void Crystal$(), and finally reaches through in surges of darkness.",
                            "Jede Fähigkeit, die du entfesselst, und jede Konvergenz, die du vollziehst, zieht den $(l)Blick der Finsternis$() auf dich. Dein Astrolabium zeigt, wie stark er auf dir ruht.$(br2)Zuerst flüstert sie nur. Dann schickt sie $(l)Echos$(), lässt den Boden um dich zu $(item)Leerenkristall$() erstarren und greift schließlich in Wogen der Dunkelheit hindurch."))),
            new Entry("star_anchors", "the_eclipse", "Star Anchors", "Sternenanker", "astralvigil:star_anchor", 1, null, List.of(
                    new Crafting("astralvigil:star_anchor", "", ""),
                    new Text(
                            "Place $(item)Star Anchors$() under the open sky in the shape of a constellation you have learned – the same shape as on your Star Chart, any rotation, mirrored or not, at one, two or three times the size. Fill every anchor with starlight, then touch one.$(br2)The ritual blesses everyone nearby and pushes the Gaze back. Larger layouts last longer and soothe more.",
                            "Setze $(item)Sternenanker$() unter offenem Himmel in der Form eines erlernten Sternbilds – dieselbe Form wie auf deiner Sternkarte, beliebig gedreht, gespiegelt oder nicht, in ein-, zwei- oder dreifacher Größe. Fülle jeden Anker mit Sternenlicht und berühre dann einen.$(br2)Das Ritual segnet alle in der Nähe und drängt den Blick zurück. Größere Anordnungen wirken länger und lindern mehr."))),
            new Entry("observatories", "the_eclipse", "Fallen Observatories", "Gefallene Observatorien", "astralvigil:constellation_lock", 2, null, List.of(
                    new Text(
                            "Ruined towers of dark stone dot the land. Their vaults are sealed with $(item)Constellation Locks$() – touch one after learning its sign and it dissolves. Pages of my lost chronicle wait inside.",
                            "Verfallene Türme aus dunklem Stein sind über das Land verstreut. Ihre Gewölbe sind mit $(item)Sternbildschlössern$() versiegelt – berühre eines, nachdem du sein Zeichen erlernt hast, und es löst sich auf. Darin warten Seiten meiner verlorenen Chronik."))),
            // ---------------------------------------------------------------- firmament
            new Entry("ascension", "firmament", "Ascension", "Aufstieg", "astralvigil:ascension_key", 0, null, List.of(
                    new Spotlight("astralvigil:ascension_key",
                            "Forged at the Convergence Altar: an Astral Core as catalyst; Starmetal, Sunsteel and Moonsilver, a Void Shard and two feathers as offerings; 2000 pure starlight. The key alone carries no one - it lights an Astral Gate.",
                            "Am Konvergenzaltar geschmiedet: ein Astralkern als Katalysator; Sternenstahl, Sonnenstahl, Mondsilber, ein Leerensplitter und zwei Federn als Gaben; 2000 reines Sternenlicht. Der Schlüssel allein trägt niemanden - er entzündet ein Astraltor."),
                    new Spotlight("astralvigil:astral_gate",
                            "Build a closed frame standing upright, like a door: the opening at least 2 wide and 3 tall (corners are optional). Set the Ascension Key into the frame and step into the light for a few heartbeats.$(br2)The frame decides where you arrive. An $(item)Astral Gate$() leads to the hub; each Watcher has a gate of its own.",
                            "Baue einen geschlossenen, aufrecht stehenden Rahmen, wie eine Tür: die Öffnung mindestens 2 breit und 3 hoch (Ecken sind freiwillig). Setze den Aufstiegsschlüssel in den Rahmen und tritt für einige Herzschläge ins Licht.$(br2)Der Rahmen bestimmt, wo du ankommst. Ein $(item)Astraltor$() führt zur Mitte; jeder Wächter hat ein eigenes Tor."),
                    new Text(
                            "$(item)Solar Gate$() (Sunsteel, gold blocks) - Solenne's Sun Spire.$(br)$(item)Lunar Gate$() (Moonsilver, quartz) - Vael's Moon Arena.$(br)$(item)Prism Gate$() (Starmetal, diamond blocks) - Orrin's Prism Observatory.$(br)$(item)Archive Gate$() (amethyst, bookshelves) - Ilyra's Endless Library.$(br)$(item)Umbral Gate$() (crying obsidian, obsidian) - the Umbral Throne; it opens only for one who carries all four Marks.",
                            "$(item)Sonnentor$() (Sonnenstahl, Goldblöcke) - Solennes Sonnenspitze.$(br)$(item)Mondtor$() (Mondsilber, Quarz) - Vaels Mondarena.$(br)$(item)Prismentor$() (Sternenstahl, Diamantblöcke) - Orrins Prismenobservatorium.$(br)$(item)Archivtor$() (Amethyst, Bücherregale) - Ilyras Endlose Bibliothek.$(br)$(item)Umbraltor$() (weinender Obsidian, Obsidian) - der Umbralthron; es öffnet sich nur dem, der alle vier Zeichen trägt."),
                    new Text(
                            "The Firmament is light: you fall slowly and land softly. Fall off the islands and the sky catches you and sets you down at the nearest arrival platform. Every platform has a gate that leads home.",
                            "Das Firmament ist leicht: Du fällst langsam und landest sanft. Stürzt du von den Inseln, fängt dich der Himmel auf und setzt dich am nächsten Ankunftsplatz ab. Jeder Platz hat ein Tor, das nach Hause führt."))),
            new Entry("sanctums", "firmament", "The Sanctums", "Die Heiligtümer", "astralvigil:mark_of_solenne", 1, null, List.of(
                    new Text(
                            "Four trial islands float far apart, each a day's labour and each reached only through its own gate: Solenne's Sun Spire, Vael's Moon Arena, Orrin's Prism Observatory and Ilyra's Endless Library. Each Watcher waits beside its arrival platform - speak to it to begin its trial.$(br2)Each completed trial grants that Watcher's $(l)Mark$() and its $(l)favour$() – but rival Watchers grow cooler: Solenne and Vael, Orrin and Ilyra.",
                            "Vier Prüfungsinseln schweben weit voneinander entfernt, jede ein Tagewerk und jede nur durch ihr eigenes Tor erreichbar: Solennes Sonnenspitze, Vaels Mondarena, Orrins Prismenobservatorium und Ilyras Endlose Bibliothek. Jeder Wächter wartet neben seinem Ankunftsplatz - sprich ihn an, um seine Prüfung zu beginnen.$(br2)Jede bestandene Prüfung gewährt das $(l)Zeichen$() des Wächters und seine $(l)Gunst$() – doch rivalisierende Wächter kühlen ab: Solenne und Vael, Orrin und Ilyra."))),
            new Entry("eclipse", "firmament", "The Eclipse", "Die Finsternis", "astralvigil:umbral_sigil", 2, null, List.of(
                    new Spotlight("astralvigil:umbral_sigil",
                            "Forged at the Convergence Altar from a Void Shard, an Astral Core, Void Crystals, obsidian and an echo shard with 3000 Stella light. Use it on the $(l)Umbral Throne$() - reached only through an Umbral Gate - while carrying all four Marks to call down the Eclipse.",
                            "Am Konvergenzaltar aus einem Leerensplitter, einem Astralkern, Leerenkristallen, Obsidian und einer Echoscherbe mit 3000 Stella-Licht geschmiedet. Benutze es auf dem $(l)Umbralthron$() - nur durch ein Umbraltor erreichbar -, während du alle vier Zeichen trägst, um die Finsternis herabzurufen."),
                    new Text(
                            "The fallen fifth Watcher fights in three phases, and it names every great blow before it falls. $(l)Fallen Light$(): the Umbral Sweep rakes the ground before it, the Void Lance locks onto you - step sideways - and Falling Stars mark circles you must leave. After heavy blows it sinks to the ground: strike then.",
                            "Der gefallene fünfte Wächter kämpft in drei Phasen und nennt jeden großen Schlag, bevor er fällt. $(l)Gefallenes Licht$(): Der Umbrale Hieb fegt den Boden vor ihr, die Leerenlanze erfasst dich - weiche seitlich aus - und Fallende Sterne zeichnen Kreise, die du verlassen musst. Nach schweren Schlägen sinkt sie zu Boden: Dann schlag zu."),
                    new Text(
                            "$(l)Shattered Halo$(): four Umbral Pylons rise and shield it - destroy them all and it staggers. Echoes join, and the Shadow Dive marks a spot before it slams down; jump the shockwave. $(l)Total Eclipse$(): walls of crying obsidian rise. Hide behind them from the Umbral Nova, jump or hide from the sweeping Eclipse Ray. Near death it rages. Hiding forever or fleeing earns its Umbral Grasp.",
                            "$(l)Zerbrochener Heiligenschein$(): Vier Umbrale Pylonen erheben sich und schirmen sie ab - zerstöre sie alle, und sie taumelt. Echos greifen ein, und der Schattensturz zeichnet eine Stelle, ehe sie herabstürzt; spring über die Druckwelle. $(l)Totale Finsternis$(): Mauern aus weinendem Obsidian wachsen empor. Verbirg dich dort vor der Umbralen Nova, spring über den kreisenden Finsternisstrahl oder geh in Deckung. Dem Tode nah, rast sie. Wer sich ewig versteckt oder flieht, spürt ihren Umbralen Griff."),
                    new Spotlight("astralvigil:halo_of_dawn",
                            "From the Eclipse Heart, Sunsteel, Moonsilver, Starmetal and Seraph Feathers: the Halo of Dawn. Carried, it lowers the starlight cost of abilities and lets the Eclipse's gaze slide off you. Seraph Feathers alone bind Seraph Wings to you.",
                            "Aus dem Herz der Finsternis, Sonnenstahl, Mondsilber, Sternenstahl und Seraphsfedern: der Heiligenschein der Morgenröte. Getragen senkt er die Sternenlichtkosten von Fähigkeiten und lässt den Blick der Finsternis an dir abgleiten. Seraphsfedern allein binden dir Seraphsflügel."))),
            // ---------------------------------------------------------------- constellations
            new Entry("constellations", "constellations", "Signs in the Sky", "Zeichen am Himmel", "astralvigil:star_chart", 0, null, List.of(
                    new Text(
                            "Each Watcher left a sign in the sky. Learn them on your $(item)Star Chart$(). One day you will build them below, stone by stone – and the sky will answer.",
                            "Jeder Wächter hinterließ ein Zeichen am Himmel. Erlerne sie auf deiner $(item)Sternkarte$(). Eines Tages wirst du sie unten nachbauen, Stein um Stein – und der Himmel wird antworten."))),
            new Entry("halo", "constellations", "The Halo", "Der Heiligenschein", "minecraft:sunflower", 1, "research/constellation_halo", List.of(
                    new Text(
                            "Solenne's ring: six stars around an unlit core. The core is not part of the sign – it is where the sun would stand.",
                            "Solennes Ring: sechs Sterne um einen unbeleuchteten Kern. Der Kern gehört nicht zum Zeichen – dort stünde die Sonne."))),
            new Entry("crescent", "constellations", "The Crescent", "Die Sichel", "minecraft:snow_block", 2, "research/constellation_crescent", List.of(
                    new Text(
                            "Vael's arc, five stars bent like a drawn bow.",
                            "Vaels Bogen, fünf Sterne, gekrümmt wie eine gespannte Sehne."))),
            new Entry("anvil", "constellations", "The Anvil", "Der Amboss", "minecraft:anvil", 3, "research/constellation_anvil", List.of(
                    new Text(
                            "Orrin's anvil: a broad face, a narrow waist, a steady foot. Every star he ever forged was struck upon it.",
                            "Orrins Amboss: eine breite Bahn, eine schmale Taille, ein fester Fuß. Jeder Stern, den er je schmiedete, wurde darauf geschlagen."))),
            new Entry("quill", "constellations", "The Quill", "Die Feder", "minecraft:feather", 4, "research/constellation_quill", List.of(
                    new Text(
                            "My own sign. A long shaft and a feathered tip – it only shows itself to those who read both moon and stars.",
                            "Mein eigenes Zeichen. Ein langer Kiel und eine gefiederte Spitze – es zeigt sich nur jenen, die Mond und Sterne zugleich lesen."))));

    static {
        // one Chronicle entry per lore fragment, unlocked by reading it
        List<Entry> pages = new java.util.ArrayList<>(ENTRIES_BASE);
        for (int i = 0; i < LoreContent.PAGES.size(); i++) {
            LoreContent.Page p = LoreContent.PAGES.get(i);
            pages.add(new Entry("fragment_" + (i + 1), "chronicle", (i + 1) + ". " + p.titleEn(), (i + 1) + ". " + p.titleDe(),
                    "astralvigil:lore_fragment", i, "lore/fragment_" + (i + 1), List.of(new Text(p.en(), p.de()))));
        }
        ENTRIES = List.copyOf(pages);
    }
}
