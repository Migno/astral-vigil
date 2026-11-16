package media.hofferbert.astralvigil.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.compat.PatchouliCompat;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Generates the Patchouli book "Codex Caelestis" (book.json + categories + entries for en_us and de_de) from
 * {@link CodexContent}. Patchouli is optional at runtime; the files are inert without it.
 */
public class AVCodexProvider implements DataProvider {
    private final PackOutput output;

    public AVCodexProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Path data = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(AstralVigil.MOD_ID)
                .resolve("patchouli_books")
                .resolve(PatchouliCompat.BOOK);
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(AstralVigil.MOD_ID)
                .resolve("patchouli_books")
                .resolve(PatchouliCompat.BOOK);

        tasks.add(save(cache, book(), data.resolve("book.json")));

        for (String lang : new String[] {"en_us", "de_de"}) {
            boolean de = lang.equals("de_de");
            Path base = assets.resolve(lang);
            for (CodexContent.Category c : CodexContent.CATEGORIES) {
                JsonObject json = new JsonObject();
                json.addProperty("name", de ? c.nameDe() : c.nameEn());
                json.addProperty("description", de ? c.descDe() : c.descEn());
                json.addProperty("icon", c.icon());
                json.addProperty("sortnum", c.sort());
                tasks.add(save(cache, json, base.resolve("categories").resolve(c.id() + ".json")));
            }
            for (CodexContent.Entry e : CodexContent.ENTRIES) {
                JsonObject json = new JsonObject();
                json.addProperty("name", de ? e.nameDe() : e.nameEn());
                json.addProperty("icon", e.icon());
                json.addProperty("category", AstralVigil.MOD_ID + ":" + e.category());
                json.addProperty("sortnum", e.sort());
                if (e.advancement() != null) {
                    json.addProperty("advancement", AstralVigil.MOD_ID + ":" + e.advancement());
                }
                JsonArray pages = new JsonArray();
                for (CodexContent.Page p : e.pages()) {
                    pages.add(p.toJson(de));
                }
                json.add("pages", pages);
                tasks.add(save(cache, json, base.resolve("entries").resolve(e.category()).resolve(e.id() + ".json")));
            }
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    private static JsonObject book() {
        JsonObject book = new JsonObject();
        book.addProperty("name", "item.astralvigil.codex_caelestis");
        book.addProperty("landing_text", "astralvigil.codex.landing");
        book.addProperty("i18n", true);
        book.addProperty("use_resource_pack", true);
        book.addProperty("dont_generate_book", true);
        book.addProperty("custom_book_item", AstralVigil.MOD_ID + ":codex_caelestis");
        book.addProperty("show_progress", true);
        book.addProperty("book_texture", "patchouli:textures/gui/book_blue.png");
        book.addProperty("nameplate_color", "F2C94C");
        book.addProperty("version", 1);
        return book;
    }

    private static CompletableFuture<?> save(CachedOutput cache, JsonElement json, Path path) {
        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public String getName() {
        return "Astral Vigil Codex Caelestis (Patchouli)";
    }
}
