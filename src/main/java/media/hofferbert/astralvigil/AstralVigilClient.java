package media.hofferbert.astralvigil;

import media.hofferbert.astralvigil.client.StarlightBeamRenderer;
import media.hofferbert.astralvigil.datagen.AVDataGen;
import media.hofferbert.astralvigil.registry.AVBlockEntities;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client-only entry point. Never loaded on dedicated servers. */
@Mod(value = AstralVigil.MOD_ID, dist = Dist.CLIENT)
public final class AstralVigilClient {
    public AstralVigilClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(AVDataGen::gather);
        modBus.addListener(AstralVigilClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(AstralVigilClient::onTooltip);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AVBlockEntities.COLLECTOR.get(), ctx -> new StarlightBeamRenderer());
        event.registerBlockEntityRenderer(AVBlockEntities.LENS.get(), ctx -> new StarlightBeamRenderer());
        event.registerBlockEntityRenderer(AVBlockEntities.MIRROR.get(), ctx -> new StarlightBeamRenderer());
        event.registerBlockEntityRenderer(AVBlockEntities.PRISM.get(), ctx -> new StarlightBeamRenderer());
    }

    /** Flavour line under every Astral Vigil item that has a {@code <descriptionId>.desc} key. */
    private static void onTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        if (item == AVItems.ASTRAL_SHARD.get()
                || item == AVItems.ASTROLABE.get()
                || item == AVItems.FALLEN_STAR.get()
                || item == AVItems.STAR_CHART.get()
                || item == AVItems.CODEX_CAELESTIS.get()
                || item == AVItems.STARLIGHT_COLLECTOR.get()
                || item == AVItems.LENS.get()
                || item == AVItems.MIRROR.get()
                || item == AVItems.PRISM.get()
                || item == AVItems.STARLIGHT_RESERVOIR.get()) {
            event.getToolTip()
                    .add(1, Component.translatable(item.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
