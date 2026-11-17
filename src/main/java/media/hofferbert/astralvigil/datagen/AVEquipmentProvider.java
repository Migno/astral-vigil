package media.hofferbert.astralvigil.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * 26.1 equipment asset {@code assets/astralvigil/equipment/seraph.json}: tells the renderer which armour layer
 * textures belong to the Seraph Plate. (1.21.1 reads {@code textures/models/armor/seraph_layer_*.png} instead.)
 */
public class AVEquipmentProvider implements DataProvider {
    private final PackOutput output;

    public AVEquipmentProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject layers = new JsonObject();
        layers.add("humanoid", layer("astralvigil:seraph"));
        layers.add("humanoid_leggings", layer("astralvigil:seraph"));
        JsonObject root = new JsonObject();
        root.add("layers", layers);
        return DataProvider.saveStable(
                cache,
                root,
                output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                        .resolve(AstralVigil.MOD_ID)
                        .resolve("equipment")
                        .resolve("seraph.json"));
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
