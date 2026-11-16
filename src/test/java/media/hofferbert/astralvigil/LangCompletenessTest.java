package media.hofferbert.astralvigil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Fails if the generated de_de.json misses (or has extra) keys compared to en_us.json. */
class LangCompletenessTest {
    private static Path langDir() {
        String generated = System.getProperty("av.generatedResources", "src/generated/resources");
        return Path.of(generated, "assets", "astralvigil", "lang");
    }

    private static JsonObject load(String locale) throws IOException {
        Path file = langDir().resolve(locale + ".json");
        assertTrue(Files.exists(file), "missing " + file + " - run runData");
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test
    void germanIsComplete() throws IOException {
        JsonObject en = load("en_us");
        JsonObject de = load("de_de");
        Set<String> missing = new TreeSet<>(en.keySet());
        missing.removeAll(de.keySet());
        Set<String> extra = new TreeSet<>(de.keySet());
        extra.removeAll(en.keySet());
        assertEquals(Set.of(), missing, "de_de is missing keys");
        assertEquals(Set.of(), extra, "de_de has keys that en_us lacks");
    }

    @Test
    void noEmptyTranslations() throws IOException {
        for (String locale : new String[] {"en_us", "de_de"}) {
            JsonObject json = load(locale);
            for (String key : json.keySet()) {
                assertTrue(!json.get(key).getAsString().isBlank(), locale + " has empty value for " + key);
            }
        }
    }

    @Test
    void placeholdersMatch() throws IOException {
        JsonObject en = load("en_us");
        JsonObject de = load("de_de");
        for (String key : en.keySet()) {
            if (!de.has(key)) {
                continue;
            }
            assertEquals(
                    count(en.get(key).getAsString()),
                    count(de.get(key).getAsString()),
                    "placeholder count differs for " + key);
        }
    }

    private static long count(String s) {
        return s.split("%s", -1).length - 1L;
    }
}
