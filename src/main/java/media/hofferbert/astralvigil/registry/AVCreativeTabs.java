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
                output.accept(AVItems.STARLIGHT_COLLECTOR.get());
                output.accept(AVItems.LENS.get());
                output.accept(AVItems.MIRROR.get());
                output.accept(AVItems.PRISM.get());
                output.accept(AVItems.STARLIGHT_RESERVOIR.get());
                output.accept(AVItems.PEDESTAL.get());
                output.accept(AVItems.CONVERGENCE_ALTAR.get());
                output.accept(AVItems.ASTRAL_FORGE.get());
                output.accept(AVItems.STARMETAL_INGOT.get());
                output.accept(AVItems.SUNSTEEL_INGOT.get());
                output.accept(AVItems.MOONSILVER_INGOT.get());
                output.accept(AVItems.ASTRAL_CORE.get());
                output.accept(AVItems.SIGIL_BLANK.get());
                output.accept(AVItems.HALO_EDGE.get());
                output.accept(AVItems.DAWNBREAKER.get());
                output.accept(AVItems.CRESCENT.get());
                output.accept(AVItems.TIDECALLER.get());
                output.accept(AVItems.STARLIGHT_AEGIS.get());
                output.accept(AVItems.SERAPH_HELMET.get());
                output.accept(AVItems.SERAPH_CHESTPLATE.get());
                output.accept(AVItems.SERAPH_LEGGINGS.get());
                output.accept(AVItems.SERAPH_BOOTS.get());
                AVItems.SIGILS.values().forEach(s -> output.accept(s.get()));
            })
            .build());

    private AVCreativeTabs() {}
}
