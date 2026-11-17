package media.hofferbert.astralvigil.client.cosmetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import media.hofferbert.astralvigil.cosmetics.Cosmetic;
import media.hofferbert.astralvigil.cosmetics.Cosmetic.Slot;
import media.hofferbert.astralvigil.cosmetics.Loadout;
import media.hofferbert.astralvigil.network.AVNetwork;
import media.hofferbert.astralvigil.registry.AVAttachments;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.joml.Quaternionf;
import org.joml.Vector3f;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.neoforge.network.PacketDistributor;
*///?}

/**
 * The Celestial Wardrobe: pick one developer cosmetic per slot (head, back, wings, aura) with a turning live preview of
 * your own player. Every change is sent to the server, which checks developer access and syncs the look to everyone.
 */
public class CosmeticsScreen extends Screen {
    private static final int W = 340;
    private static final int H = 212;
    private static final int NAVY = 0xF01E1A2E;
    private static final int NAVY_LIGHT = 0xFF2C2942;
    private static final int GOLD = 0xFFF2C94C;
    private static final int GOLD_DARK = 0xFFC9971E;
    private static final int GREY = 0xFF9C98B4;
    private static final int WHITE = 0xFFFFFFFF;

    private Loadout loadout = Loadout.EMPTY;
    private boolean access;
    private boolean spinning = true;
    private float spin = 25f;
    private final List<Button> arrows = new ArrayList<>();
    private int x0;
    private int y0;

    public CosmeticsScreen() {
        super(Component.translatable("screen.astralvigil.cosmetics"));
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2 - 10;
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player != null) {
            loadout = player.getData(AVAttachments.COSMETICS);
            access = player.getData(AVAttachments.COSMETIC_ACCESS);
        }
        arrows.clear();
        Slot[] slots = Slot.values();
        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            int y = rowY(i) + 11;
            arrows.add(addRenderableWidget(Button.builder(Component.literal("<"), b -> change(slot, -1)).bounds(x0 + 142, y, 18, 18).build()));
            arrows.add(addRenderableWidget(Button.builder(Component.literal(">"), b -> change(slot, 1)).bounds(x0 + W - 26, y, 18, 18).build()));
        }
        addRenderableWidget(Button.builder(spinLabel(), b -> {
                    spinning = !spinning;
                    b.setMessage(spinLabel());
                })
                .bounds(x0 + 10, y0 + H - 28, 58, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.astralvigil.cosmetics.turn"), b -> spin += 180f)
                .bounds(x0 + 72, y0 + H - 28, 58, 18)
                .build());
        Button clear = addRenderableWidget(Button.builder(Component.translatable("screen.astralvigil.cosmetics.clear"), b -> send(Loadout.EMPTY))
                .bounds(width / 2 - 102, y0 + H + 6, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 + 2, y0 + H + 6, 100, 20).build());
        arrows.add(clear);
        arrows.forEach(b -> b.active = access);
    }

    private Component spinLabel() {
        return Component.translatable(spinning ? "screen.astralvigil.cosmetics.spin_on" : "screen.astralvigil.cosmetics.spin_off");
    }

    private int rowY(int i) {
        return y0 + 34 + i * 41;
    }

    private void change(Slot slot, int step) {
        send(loadout.with(slot, Cosmetic.cycle(slot, loadout.get(slot), step)));
    }

    private void send(Loadout next) {
        loadout = next;
        //? if >=26.1 {
        ClientPacketDistributor.sendToServer(new AVNetwork.SetCosmetics(next));
        //?} else {
        /*PacketDistributor.sendToServer(new AVNetwork.SetCosmetics(next));
        *///?}
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.2f + minecraft.player.getRandom().nextFloat() * 0.3f);
        }
    }

    @Override
    public void tick() {
        if (spinning) {
            spin += 2f;
        }
        if (minecraft != null && minecraft.player != null) {
            access = minecraft.player.getData(AVAttachments.COSMETIC_ACCESS);
            arrows.forEach(b -> b.active = access);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- drawing (version independent)

    interface Canvas {
        void fill(int x0, int y0, int x1, int y1, int color);

        void text(Component text, int x, int y, int color);

        void player(int x0, int y0, int x1, int y1, int size, float bodyYaw);
    }

    private void centered(Canvas c, Component text, int cx, int y, int color) {
        c.text(text, cx - font.width(text) / 2, y, color);
    }

    private void frame(Canvas c, int x0, int y0, int x1, int y1, int fill, int edge) {
        c.fill(x0, y0, x1, y1, edge);
        c.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
    }

    void draw(Canvas c, float partial) {
        frame(c, x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, NAVY, GOLD_DARK);
        c.fill(x0, y0, x0 + W, y0 + 1, GOLD);
        centered(c, title, width / 2, y0 + 8, GOLD);
        Component sub = Component.translatable(access ? "screen.astralvigil.cosmetics.subtitle" : "screen.astralvigil.cosmetics.locked");
        centered(c, sub, width / 2, y0 + 20, access ? GREY : 0xFFE08A8A);

        // preview
        frame(c, x0 + 8, y0 + 34, x0 + 132, y0 + H - 32, 0xFF120F1E, NAVY_LIGHT);
        for (int i = 0; i < 14; i++) { // a few fixed background stars
            int sx = x0 + 14 + (i * 37) % 112;
            int sy = y0 + 40 + (i * 53) % 130;
            c.fill(sx, sy, sx + 1, sy + 1, i % 3 == 0 ? 0xFF8FEFFF : 0xFF5E5A78);
        }
        c.player(x0 + 9, y0 + 35, x0 + 131, y0 + H - 33, 50, spin + (spinning ? 2f * partial : 0f));

        Slot[] slots = Slot.values();
        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            int y = rowY(i);
            c.text(Component.translatable(slot.translationKey()), x0 + 142, y, GOLD);
            Optional<Cosmetic> worn = loadout.get(slot);
            frame(c, x0 + 162, y + 11, x0 + W - 28, y + 29, 0xFF16132A, worn.isPresent() ? 0xFF000000 | worn.get().colour() : NAVY_LIGHT);
            Component name = worn.<Component>map(w -> Component.translatable(w.translationKey()))
                    .orElse(Component.translatable("cosmetic.astralvigil.none"));
            centered(c, name, (x0 + 162 + x0 + W - 28) / 2, y + 16, worn.isPresent() ? WHITE : GREY);
            worn.ifPresent(w -> c.text(Component.translatable(w.translationKey() + ".desc"), x0 + 142, y + 31, GREY));
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        draw(new Canvas() {
            @Override
            public void fill(int x0, int y0, int x1, int y1, int color) {
                graphics.fill(x0, y0, x1, y1, color);
            }

            @Override
            public void text(Component text, int x, int y, int color) {
                graphics.text(font, text, x, y, color);
            }

            @Override
            public void player(int x0, int y0, int x1, int y1, int size, float bodyYaw) {
                if (minecraft == null || minecraft.player == null) {
                    return;
                }
                EntityRenderer<? super LivingEntity, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
                EntityRenderState state = renderer.createRenderState(minecraft.player, 1.0F);
                state.shadowPieces.clear();
                state.outlineColor = 0;
                if (state instanceof LivingEntityRenderState living) {
                    living.bodyRot = 180.0F + bodyYaw;
                    living.yRot = 0.0F;
                    living.xRot = 0.0F;
                    living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
                    living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
                    living.scale = 1.0F;
                }
                Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
                graphics.entity(state, size, translation, new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), x0, y0, x1, y1);
            }
        }, a);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        draw(new Canvas() {
            @Override
            public void fill(int x0, int y0, int x1, int y1, int color) {
                graphics.fill(x0, y0, x1, y1, color);
            }

            @Override
            public void text(Component text, int x, int y, int color) {
                graphics.drawString(font, text, x, y, color);
            }

            @Override
            public void player(int x0, int y0, int x1, int y1, int size, float bodyYaw) {
                if (minecraft == null || minecraft.player == null) {
                    return;
                }
                LocalPlayer p = minecraft.player;
                float body = p.yBodyRot;
                float bodyO = p.yBodyRotO;
                float yaw = p.getYRot();
                float pitch = p.getXRot();
                float head = p.yHeadRot;
                float headO = p.yHeadRotO;
                p.yBodyRot = p.yBodyRotO = 180.0F + bodyYaw;
                p.setYRot(180.0F + bodyYaw);
                p.setXRot(0.0F);
                p.yHeadRot = p.yHeadRotO = p.getYRot();
                graphics.enableScissor(x0, y0, x1, y1);
                Vector3f translation = new Vector3f(0.0F, p.getBbHeight() / 2.0F + 0.0625F * p.getScale(), 0.0F);
                InventoryScreen.renderEntityInInventory(graphics, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F, size / p.getScale(), translation,
                        new Quaternionf().rotateZ((float) Math.PI), null, p);
                graphics.disableScissor();
                p.yBodyRot = body;
                p.yBodyRotO = bodyO;
                p.setYRot(yaw);
                p.setXRot(pitch);
                p.yHeadRot = head;
                p.yHeadRotO = headO;
            }
        }, partialTick);
    }
    *///?}
}
