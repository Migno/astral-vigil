package media.hofferbert.astralvigil.gametest;

import java.util.List;
import java.util.function.Consumer;
import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

/**
 * Version-independent catalogue of all Astral Vigil GameTests.
 * Registration differs per Minecraft version, see {@link GameTestRegistration}.
 */
public final class AVGameTests {
    /** Default 5x4x5 smooth-stone platform with open sky. */
    public static final String PLATFORM = "gametest/platform";

    public record Def(String name, String structure, int maxTicks, boolean skyAccess, Consumer<GameTestHelper> body) {}

    public static final List<Def> ALL = List.of(new Def("mod_loaded", PLATFORM, 40, false, AVGameTests::modLoaded));

    private AVGameTests() {}

    static void modLoaded(GameTestHelper helper) {
        GT.check(helper, ModList.get().isLoaded(AstralVigil.MOD_ID), "Astral Vigil is not loaded");
        helper.assertBlockPresent(Blocks.SMOOTH_STONE, GT.pos(2, 0, 2));
        helper.succeed();
    }
}
