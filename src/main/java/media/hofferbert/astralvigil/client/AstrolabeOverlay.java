package media.hofferbert.astralvigil.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.compat.Keys;
import media.hofferbert.astralvigil.item.AstrolabeItem;
import media.hofferbert.astralvigil.registry.AVAttachments;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.ResearchData;
import media.hofferbert.astralvigil.research.ResearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * The Astrolabe's lens, Thaumcraft-style: while it is held or worn on the head, a small panel beside the crosshair names
 * what it points at, its celestial alignments and whether the player has researched it yet. While a scan is held, a ring
 * of starlight fills around the crosshair. Everything is read from the synced research attachment and client tags;
 * nothing is sent to the server.
 */
public final class AstrolabeOverlay {
    private static final int GOLD = 0xFFF2C94C;
    private static final int GOLD_DARK = 0xFF7A5A12;
    private static final int NAVY = 0xC01E1A2E;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFF9A98B0;
    private static final int GREEN = 0xFF7CE3A0;
    private static final int CYAN = 0xFF8FEFFF;
    private static final int CYAN_DARK = 0x802FA8D0;
    private static final int RING_DOTS = 20;

    private AstrolabeOverlay() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(AstrolabeOverlay::onRegisterLayers);
    }

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, AstralVigil.id("astrolabe"),
                (graphics, delta) -> render(painter(graphics), delta.getGameTimeDeltaPartialTick(false)));
    }

    interface Painter {
        void fill(int x0, int y0, int x1, int y1, int color);

        void text(Component text, int x, int y, int color);

        int width(Component text);
    }

    //? if >=26.1 {
    static Painter painter(GuiGraphicsExtractor g) {
        var font = Minecraft.getInstance().font;
        return new Painter() {
            public void fill(int x0, int y0, int x1, int y1, int color) {
                g.fill(x0, y0, x1, y1, color);
            }

            public void text(Component text, int x, int y, int color) {
                g.text(font, text, x, y, color, true);
            }

            public int width(Component text) {
                return font.width(text);
            }
        };
    }
    //?} else {
    /*static Painter painter(GuiGraphics g) {
        var font = Minecraft.getInstance().font;
        return new Painter() {
            public void fill(int x0, int y0, int x1, int y1, int color) {
                g.fill(x0, y0, x1, y1, color);
            }

            public void text(Component text, int x, int y, int color) {
                g.drawString(font, text, x, y, color, true);
            }

            public int width(Component text) {
                return font.width(text);
            }
        };
    }
    *///?}

    /** What the lens shows: a name, the research subject key (null: nothing to research here) and its alignments. */
    record Reading(Component name, String subject, Set<Alignment> alignments) {}

    static Reading read(Player player, float partialTick) {
        HitResult hit = AstrolabeItem.target(player, partialTick);
        if (hit instanceof EntityHitResult e) {
            var type = e.getEntity().getType();
            return new Reading(type.getDescription(), "entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(type),
                    ResearchManager.alignmentsOf(e.getEntity()));
        }
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = player.level().getBlockState(b.getBlockPos());
            return new Reading(state.getBlock().getName(), "block/" + BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                    ResearchManager.alignmentsOf(state));
        }
        BlockPos pos = player.blockPosition();
        if (!player.level().canSeeSky(pos.above())) {
            return new Reading(Component.translatable("hud.astralvigil.astrolabe.no_sky"), null, Set.of());
        }
        var biome = player.level().getBiome(pos);
        var key = biome.unwrapKey().map(Keys::id).orElse(null);
        if (key == null) {
            return new Reading(Component.translatable("hud.astralvigil.astrolabe.no_sky"), null, Set.of());
        }
        Component name = Component.translatable("hud.astralvigil.astrolabe.sky",
                Component.translatable("biome." + key.getNamespace() + "." + key.getPath()));
        return new Reading(name, "biome/" + key, ResearchManager.alignmentsOf(biome));
    }

    static void render(Painter p, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        //? if >=26.2 {
        /*boolean hidden = mc.gui.hud.isHidden();
        *///?} else {
        boolean hidden = mc.options.hideGui;
        //?}
        if (player == null || mc.level == null || hidden || !AstrolabeItem.active(player)) {
            return;
        }
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;

        if (player.isUsingItem() && player.getUseItem().getItem() instanceof AstrolabeItem) {
            ring(p, cx, cy, Math.min(1f, (player.getTicksUsingItem() + partialTick) / AstrolabeItem.SCAN_TICKS));
        }

        Reading r = read(player, partialTick);
        ResearchData data = player.getData(AVAttachments.RESEARCH.get());
        List<Component> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add(r.name());
        colors.add(WHITE);
        if (r.subject() != null) {
            if (!r.alignments().isEmpty()) {
                MutableComponent aligns = Component.empty();
                boolean first = true;
                for (Alignment a : r.alignments()) {
                    if (!first) {
                        aligns.append(Component.literal("  "));
                    }
                    aligns.append(Component.literal("◆ ").append(Component.translatable(a.translationKey())).withColor(a.color()));
                    first = false;
                }
                lines.add(aligns);
                colors.add(WHITE);
            }
            if (data.hasScanned(r.subject())) {
                lines.add(Component.translatable("hud.astralvigil.astrolabe.known"));
                colors.add(GREEN);
            } else if (r.alignments().isEmpty()) {
                lines.add(Component.translatable("hud.astralvigil.astrolabe.silent"));
                colors.add(GREY);
            } else {
                lines.add(Component.translatable("hud.astralvigil.astrolabe.unknown"));
                colors.add(GOLD);
            }
        }

        int w = 0;
        for (Component line : lines) {
            w = Math.max(w, p.width(line));
        }
        int x = cx + 14;
        int y = cy - 6 - lines.size() * 5;
        int h = lines.size() * 10 + 4;
        p.fill(x - 1, y - 1, x + w + 7, y + h + 1, GOLD_DARK);
        p.fill(x, y, x + w + 6, y + h, NAVY);
        p.fill(x, y, x + 1, y + h, GOLD);
        for (int i = 0; i < lines.size(); i++) {
            p.text(lines.get(i), x + 4, y + 3 + i * 10, colors.get(i));
        }
    }

    /** Scan progress: a ring of starlight motes around the crosshair, lit clockwise from the top. */
    static void ring(Painter p, int cx, int cy, float progress) {
        int lit = Math.round(progress * RING_DOTS);
        for (int i = 0; i < RING_DOTS; i++) {
            double angle = Math.PI * 2 * i / RING_DOTS - Math.PI / 2;
            int x = cx + (int) Math.round(Math.cos(angle) * 10);
            int y = cy + (int) Math.round(Math.sin(angle) * 10);
            int color = i < lit ? CYAN : CYAN_DARK;
            p.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }
}
