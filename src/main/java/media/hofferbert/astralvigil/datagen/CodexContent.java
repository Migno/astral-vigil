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
                    "constellations",
                    "Constellations",
                    "Sternbilder",
                    "The Watchers' signs, written in stars.",
                    "Die Zeichen der Wächter, in Sterne geschrieben.",
                    "astralvigil:star_chart",
                    3));

    public static final List<Entry> ENTRIES = List.of(
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
}
