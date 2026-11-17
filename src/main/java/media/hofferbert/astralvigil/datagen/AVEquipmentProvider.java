package media.hofferbert.astralvigil.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.armor.ArmorSet;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * 26.1 equipment assets {@code assets/astralvigil/equipment/<set>.json}: tell the renderer which armour layer
 * textures belong to each celestial set. (1.21.1 reads {@code textures/models/armor/<set>_layer_*.png} instead.)
 */
public class AVEquipmentProvider implements DataProvider {
    private final PackOutput output;

    public AVEquipmentProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (ArmorSet set : ArmorSet.values()) {
            JsonObject layers = new JsonObject();
            layers.add("humanoid", layer(AstralVigil.MOD_ID + ":" + set.id()));
            layers.add("humanoid_leggings", layer(AstralVigil.MOD_ID + ":" + set.id()));
            JsonObject root = new JsonObject();
            root.add("layers", layers);
            futures.add(DataProvider.saveStable(
                    cache,
                    root,
                    output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                            .resolve(AstralVigil.MOD_ID)
                            .resolve("equipment")
                            .resolve(set.id() + ".json")));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonArray layer(String texture) {
        JsonObject entry = new JsonObject();
        entry.addProperty("texture", texture);
        JsonArray array = new JsonArray();
        array.add(entry);
        return array;
    }

    @Override
    public String getName() {
        return "Astral Vigil equipment assets";
    }
}
