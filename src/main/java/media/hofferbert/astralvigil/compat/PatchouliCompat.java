package media.hofferbert.astralvigil.compat;

import java.lang.reflect.Method;
import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Optional Patchouli integration via reflection: no compile-time dependency, works with every Patchouli version that
 * exposes {@code PatchouliAPI.get().openBookGUI(ServerPlayer, <id>)}.
 */
public final class PatchouliCompat {
    public static final String BOOK = "codex_caelestis";

    private PatchouliCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded("patchouli");
    }

    public static boolean openCodex(ServerPlayer player) {
        if (!isLoaded()) {
            return false;
        }
        try {
            Class<?> api = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object instance = api.getMethod("get").invoke(null);
            for (Method m : instance.getClass().getMethods()) {
                if (m.getName().equals("openBookGUI")
                        && m.getParameterCount() == 2
                        && m.getParameterTypes()[0].isAssignableFrom(ServerPlayer.class)) {
                    m.invoke(instance, player, AstralVigil.id(BOOK));
                    return true;
                }
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            AstralVigil.LOGGER.warn("Could not open the Codex through Patchouli", e);
        }
        return false;
    }
}
