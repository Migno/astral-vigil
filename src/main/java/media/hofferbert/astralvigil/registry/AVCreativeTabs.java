package media.hofferbert.astralvigil.registry;

import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralVigil.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.astralvigil"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> AVItems.ASTRAL_SHARD.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(AVItems.ASTRAL_SHARD.get());
                output.accept(AVItems.ASTROLABE.get());
                output.accept(AVItems.STAR_CHART.get());
                output.accept(AVItems.CODEX_CAELESTIS.get());
                output.accept(AVItems.FALLEN_STAR.get());
                output.accept(AVItems.METEORIC_STONE.get());
            })
            .build());

    private AVCreativeTabs() {}
}
