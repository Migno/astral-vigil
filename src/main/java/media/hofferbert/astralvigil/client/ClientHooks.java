package media.hofferbert.astralvigil.client;

import net.minecraft.client.Minecraft;

/**
 * Entry points from common code into client-only code. Only ever invoked on the logical client (callers check
 * {@code level.isClientSide()}), so the referenced client classes are never loaded on a dedicated server.
 */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openStarChart() {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.setScreen(new StarChartScreen());
        *///?} else {
        Minecraft.getInstance().setScreen(new StarChartScreen());
        //?}
    }
}
