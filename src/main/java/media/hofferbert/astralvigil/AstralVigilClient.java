package media.hofferbert.astralvigil;

import media.hofferbert.astralvigil.client.ClientRecipes;
import media.hofferbert.astralvigil.client.FloatingItemsRenderer;
import media.hofferbert.astralvigil.client.GearClient;
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
        ClientRecipes.register();
        GearClient.register(modBus);
        media.hofferbert.astralvigil.client.SeraphWings.register(modBus);
        media.hofferbert.astralvigil.client.cosmetics.CosmeticsClient.register(modBus);
        media.hofferbert.astralvigil.client.armor.ArmorOrnaments.register(modBus);
        media.hofferbert.astralvigil.client.armor.ArmorTooltip.register();
        NeoForge.EVENT_BUS.addListener(AstralVigilClient::onTooltip);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AVBlockEntities.COLLECTOR.get(), ctx -> new StarlightBeamRenderer());
        event.registerBlockEntityRenderer(AVBlockEntities.LENS.get(), ctx -> new StarlightBeamRenderer());
        event.registerBlockEntityRenderer(AVBlockEntities.MIRROR.get(), ctx -> new StarlightBeamRenderer());
        event.registerBlockEntityRenderer(AVBlockEntities.PRISM.get(), ctx -> new StarlightBeamRenderer());
        event.registerBlockEntityRenderer(AVBlockEntities.PEDESTAL.get(), FloatingItemsRenderer::new);
        event.registerBlockEntityRenderer(AVBlockEntities.CONVERGENCE_ALTAR.get(), FloatingItemsRenderer::new);
        event.registerBlockEntityRenderer(AVBlockEntities.ASTRAL_FORGE.get(), FloatingItemsRenderer::new);
        event.registerBlockEntityRenderer(AVBlockEntities.STARLIGHT_FONT.get(), ctx -> new StarlightBeamRenderer());
        // Watchers: shared GeckoLib rig, per-Watcher texture + emissive glowmask
        media.hofferbert.astralvigil.registry.AVEntities.WATCHERS.forEach((w, type) -> event.registerEntityRenderer(
                type.get(), ctx -> media.hofferbert.astralvigil.client.WatcherRenderer.create(ctx, w)));
        // Eclipse + Umbral Pylon: generated rigs (tools/dev/gen_eclipse.py) with emissive glowmasks
        event.registerEntityRenderer(media.hofferbert.astralvigil.registry.AVEntities.ECLIPSE.get(), media.hofferbert.astralvigil.client.EclipseRenderer::eclipse);
        event.registerEntityRenderer(media.hofferbert.astralvigil.registry.AVEntities.UMBRAL_PYLON.get(), media.hofferbert.astralvigil.client.EclipseRenderer::pylon);
        event.registerEntityRenderer(media.hofferbert.astralvigil.registry.AVEntities.ECLIPSE_ECHO.get(), media.hofferbert.astralvigil.client.EclipseRenderer::echo);
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
                || item == AVItems.STARLIGHT_RESERVOIR.get()
                || item == AVItems.PEDESTAL.get()
                || item == AVItems.CONVERGENCE_ALTAR.get()
                || item == AVItems.ASTRAL_FORGE.get()
                || item == AVItems.STAR_ANCHOR.get()
                || item == AVItems.ASCENSION_KEY.get()
                || item == AVItems.UMBRAL_SIGIL.get()
                || item == AVItems.ECLIPSE_HEART.get()
                || item == AVItems.SERAPH_FEATHER.get()
                || item == AVItems.HALO_OF_DAWN.get()) {
            event.getToolTip()
                    .add(1, Component.translatable(item.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
