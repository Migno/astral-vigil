package media.hofferbert.astralvigil;

import media.hofferbert.astralvigil.command.AVCommands;
import media.hofferbert.astralvigil.config.AVClientConfig;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.gametest.GameTestRegistration;
import media.hofferbert.astralvigil.network.AVNetwork;
import media.hofferbert.astralvigil.registry.AVAttachments;
import media.hofferbert.astralvigil.registry.AVBlockEntities;
import media.hofferbert.astralvigil.gear.GearEvents;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVComponents;
//? if <26.1 {
/*import media.hofferbert.astralvigil.gear.AVMaterials;
*///?}
import media.hofferbert.astralvigil.registry.AVRecipes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import media.hofferbert.astralvigil.registry.AVCreativeTabs;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.starfall.StarfallManager;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AstralVigil.MOD_ID)
public final class AstralVigil {
    public static final String MOD_ID = "astralvigil";
    public static final Logger LOGGER = LoggerFactory.getLogger("Astral Vigil");
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.1.2";

    public AstralVigil(IEventBus modBus, ModContainer container) {
        LOGGER.info("Astral Vigil {} initialising for Minecraft {}", VERSION, MINECRAFT);

        container.registerConfig(ModConfig.Type.SERVER, AVServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, AVClientConfig.SPEC);

        AVBlocks.BLOCKS.register(modBus);
        AVItems.ITEMS.register(modBus);
        AVCreativeTabs.TABS.register(modBus);
        AVAttachments.ATTACHMENTS.register(modBus);
        AVComponents.COMPONENTS.register(modBus);
        //? if <26.1 {
        /*AVMaterials.ARMOR_MATERIALS.register(modBus);
        *///?}
        AVBlockEntities.BLOCK_ENTITIES.register(modBus);
        AVRecipes.register(modBus);
        media.hofferbert.astralvigil.registry.AVEntities.register(modBus);
        //? if >=26.1 {
        // 26.1 no longer syncs recipes to clients; JEI needs ours
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> e.sendRecipes(AVRecipes.CONVERGENCE.get(), AVRecipes.FORGE.get()));
        //?}
        AVNetwork.register(modBus);

        StarfallManager.register();
        GearEvents.register();
        media.hofferbert.astralvigil.armor.SetBonusEvents.register();
        media.hofferbert.astralvigil.eclipse.GazeManager.register();
        media.hofferbert.astralvigil.firmament.Firmament.register();
        media.hofferbert.astralvigil.cosmetics.CosmeticsManager.register();
        AVCommands.register();
        GameTestRegistration.register(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
