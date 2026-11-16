package media.hofferbert.astralvigil.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.minecraft.network.chat.Component;
//?} else {
/*import net.minecraft.gametest.framework.GameTestAssertException;
*///?}

/** Small assertion helpers that hide API differences between GameTest versions. */
public final class GT {
    private GT() {}

    /**
     * Position relative to the test structure's origin. On 1.21.1 relative (0,0,0) is the structure block
     * itself and the structure starts one block above; on 26.1 (0,0,0) is the structure origin.
     */
    public static BlockPos pos(int x, int y, int z) {
        //? if >=26.1 {
        return new BlockPos(x, y, z);
        //?} else {
        /*return new BlockPos(x, y + 1, z);
        *///?}
    }

    public static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw fail(helper, message);
        }
    }

    public static RuntimeException fail(GameTestHelper helper, String message) {
        //? if >=26.1 {
        return helper.assertionException(Component.literal(message));
        //?} else {
        /*return new GameTestAssertException(message);
        *///?}
    }
}
