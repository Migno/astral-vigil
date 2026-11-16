package media.hofferbert.astralvigil;

import media.hofferbert.astralvigil.gametest.GameTestRegistration;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
        GameTestRegistration.register(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
