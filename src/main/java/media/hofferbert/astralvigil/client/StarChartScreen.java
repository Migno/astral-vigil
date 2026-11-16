package media.hofferbert.astralvigil.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.network.AVNetwork;
import media.hofferbert.astralvigil.registry.AVAttachments;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.Constellation;
import media.hofferbert.astralvigil.research.Constellation.Edge;
import media.hofferbert.astralvigil.research.Constellation.Star;
import media.hofferbert.astralvigil.research.Constellations;
import media.hofferbert.astralvigil.research.ResearchData;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
*///?}

/**
 * Star Chart minigame: pick a revealed constellation on the left, then connect its stars on the chart (click a star,
 * then another to draw or erase the line). Decoy stars belong to no line. When the drawing matches, the constellation
 * is learned (server re-validates).
 */
public class StarChartScreen extends Screen {
    private static final int NAVY = 0xFF1E1A2E;
    private static final int NAVY_LIGHT = 0xFF2C2942;
    private static final int GOLD = 0xFFF2C94C;
    private static final int GOLD_DARK = 0xFFC9971E;
    private static final int CYAN = 0xFF8FEFFF;
    private static final int CYAN_DARK = 0xFF2FA8D0;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFF8A86A0;
    private static final int LIST_WIDTH = 120;
    private static final int ROW_HEIGHT = 18;

    private Constellation selected;
    private final Set<Edge> drawn = new HashSet<>();
    private int pendingStar = -1;
    private int successTicks;
    private int ticks;

    public StarChartScreen() {
        super(Component.translatable("screen.astralvigil.star_chart"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.astralvigil.star_chart.clear"), b -> {
                    drawn.clear();
                    pendingStar = -1;
                })
                .bounds(8, height - 26, 60, 20)
                .build());
    }

    @Override
    public void tick() {
        ticks++;
        if (successTicks > 0) {
            successTicks--;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ResearchData research() {
        return minecraft != null && minecraft.player != null
                ? minecraft.player.getData(AVAttachments.RESEARCH)
                : ResearchData.EMPTY;
    }

    private static int threshold() {
        return AVServerConfig.SPEC.isLoaded() ? AVServerConfig.RESEARCH_ATTUNEMENT.get() : 12;
    }

    // ---------------------------------------------------------------- layout

    private int chartSize() {
        return Math.max(80, Math.min(width - LIST_WIDTH - 40, height - 84));
    }

    private int chartX() {
        return LIST_WIDTH + 20 + (width - LIST_WIDTH - 20 - chartSize()) / 2;
    }

    private int chartY() {
        return 54;
    }

    private List<Star> points(Constellation c) {
        List<Star> all = new ArrayList<>(c.stars());
        all.addAll(c.decoys());
        return all;
    }

    private int px(Star s) {
        return chartX() + s.x() * chartSize() / 100;
    }

    private int py(Star s) {
        return chartY() + s.y() * chartSize() / 100;
    }

    // ---------------------------------------------------------------- drawing

    private void draw(Canvas g, int mouseX, int mouseY) {
        ResearchData data = research();
        int threshold = threshold();

        g.text(title, 10, 10, GOLD);
        int ax = 10;
        for (Alignment a : Alignment.values()) {
            Component label = Component.translatable(a.translationKey())
                    .append(" " + Math.min(data.knowledge(a), threshold) + "/" + threshold);
            g.text(label, ax, 24, 0xFF000000 | a.color());
            ax += font.width(label) + 12;
        }

        // constellation list
        int y = 54;
        for (Constellation c : Constellations.all()) {
            boolean available = Constellations.available(c, data, threshold);
            boolean learned = data.hasConstellation(c.id());
            boolean hover = mouseX >= 8 && mouseX < LIST_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;
            g.fill(8, y, LIST_WIDTH, y + ROW_HEIGHT - 2, c == selected ? NAVY_LIGHT : hover ? 0xFF25213A : NAVY);
            Component name = available || learned
                    ? Component.translatable(c.translationKey())
                    : Component.translatable("screen.astralvigil.star_chart.unknown");
            g.text(name, 12, y + 5, learned ? GOLD : available ? WHITE : GREY);
            if (learned) {
                g.text(Component.literal("✦"), LIST_WIDTH - 12, y + 5, GOLD);
            }
            y += ROW_HEIGHT;
        }

        // chart
        int cx = chartX();
        int cy = chartY();
        int size = chartSize();
        g.fill(cx - 6, cy - 6, cx + size + 6, cy + size + 6, GOLD_DARK);
        g.fill(cx - 5, cy - 5, cx + size + 5, cy + size + 5, NAVY);
        // faint background stars (deterministic)
        for (int i = 0; i < 60; i++) {
            int sx = cx + (int) ((i * 73L) % size);
            int sy = cy + (int) ((i * 151L + 37) % size);
            g.fill(sx, sy, sx + 1, sy + 1, (i + ticks / 20) % 7 == 0 ? 0xFF5A5680 : 0xFF3D3959);
        }

        if (selected == null) {
            g.centered(Component.translatable("screen.astralvigil.star_chart.pick"), cx + size / 2, cy + size / 2, GREY);
            return;
        }

        boolean learned = data.hasConstellation(selected.id());
        List<Star> pts = points(selected);
        Set<Edge> lines = learned ? Set.copyOf(selected.edges()) : drawn;
        for (Edge e : lines) {
            line(g, px(pts.get(e.a())), py(pts.get(e.a())), px(pts.get(e.b())), py(pts.get(e.b())), learned ? GOLD : CYAN_DARK);
        }
        if (pendingStar >= 0) {
            line(g, px(pts.get(pendingStar)), py(pts.get(pendingStar)), mouseX, mouseY, 0x80F2C94C);
        }
        int hovered = starAt(mouseX, mouseY);
        for (int i = 0; i < pts.size(); i++) {
            Star s = pts.get(i);
            int x = px(s);
            int yy = py(s);
            boolean twinkle = ((ticks + i * 7) / 10) % 6 == 0;
            int core = i == pendingStar || i == hovered ? GOLD : WHITE;
            g.fill(x - 1, yy - 3, x + 2, yy + 4, twinkle ? CYAN : CYAN_DARK);
            g.fill(x - 3, yy - 1, x + 4, yy + 2, twinkle ? CYAN : CYAN_DARK);
            g.fill(x - 1, yy - 1, x + 2, yy + 2, core);
        }

        Component status;
        int color;
        if (learned) {
            status = Component.translatable("screen.astralvigil.star_chart.learned");
            color = GOLD;
        } else if (successTicks > 0) {
            status = Component.translatable("screen.astralvigil.star_chart.success");
            color = GOLD;
        } else {
            status = Component.translatable("screen.astralvigil.star_chart.hint");
            color = GREY;
        }
        g.centered(Component.translatable(selected.translationKey()), cx + size / 2, cy - 17, WHITE);
        g.centered(status, cx + size / 2, cy + size + 10, color);
    }

    private static void line(Canvas g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int x = steps == 0 ? x0 : x0 + (x1 - x0) * i / steps;
            int y = steps == 0 ? y0 : y0 + (y1 - y0) * i / steps;
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    // ---------------------------------------------------------------- input

    private int starAt(double mx, double my) {
        if (selected == null) {
            return -1;
        }
        List<Star> pts = points(selected);
        for (int i = 0; i < pts.size(); i++) {
            if (Math.abs(px(pts.get(i)) - mx) <= 5 && Math.abs(py(pts.get(i)) - my) <= 5) {
                return i;
            }
        }
        return -1;
    }

    private boolean handleClick(double mx, double my, int button) {
        ResearchData data = research();
        // list
        int y = 54;
        for (Constellation c : Constellations.all()) {
            if (mx >= 8 && mx < LIST_WIDTH && my >= y && my < y + ROW_HEIGHT) {
                if (Constellations.available(c, data, threshold()) || data.hasConstellation(c.id())) {
                    selected = c;
                    drawn.clear();
                    pendingStar = -1;
                    click();
                }
                return true;
            }
            y += ROW_HEIGHT;
        }
        if (selected == null || data.hasConstellation(selected.id())) {
            return false;
        }
        if (button == 1) {
            pendingStar = -1;
            return true;
        }
        int star = starAt(mx, my);
        if (star < 0) {
            pendingStar = -1;
            return false;
        }
        if (pendingStar < 0 || pendingStar == star) {
            pendingStar = star == pendingStar ? -1 : star;
            click();
            return true;
        }
        Edge edge = new Edge(pendingStar, star);
        if (!drawn.remove(edge)) {
            drawn.add(edge);
        }
        pendingStar = star;
        click();
        if (selected.matches(drawn)) {
            successTicks = 60;
            pendingStar = -1;
            //? if >=26.1 {
            ClientPacketDistributor.sendToServer(new AVNetwork.SolveConstellation(selected.id()));
            //?} else {
            /*PacketDistributor.sendToServer(new AVNetwork.SolveConstellation(selected.id()));
            *///?}
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
            }
        }
        return true;
    }

    private void click() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.AMETHYST_CLUSTER_STEP, 0.6f, Mth.clamp(1.2f + drawn.size() * 0.1f, 1.0f, 2.0f));
        }
    }

    // ---------------------------------------------------------------- version glue

    /** Minimal drawing surface so the screen logic is written once. */
    interface Canvas {
        void fill(int x0, int y0, int x1, int y1, int color);

        void text(Component text, int x, int y, int color);

        void centered(Component text, int x, int y, int color);
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        draw(
                new Canvas() {
                    @Override
                    public void fill(int x0, int y0, int x1, int y1, int color) {
                        graphics.fill(x0, y0, x1, y1, color);
                    }

                    @Override
                    public void text(Component text, int x, int y, int color) {
                        graphics.text(font, text, x, y, color);
                    }

                    @Override
                    public void centered(Component text, int x, int y, int color) {
                        graphics.text(font, text, x - font.width(text) / 2, y, color);
                    }
                },
                mouseX,
                mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        return handleClick(event.x(), event.y(), event.button());
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        draw(
                new Canvas() {
                    @Override
                    public void fill(int x0, int y0, int x1, int y1, int color) {
                        graphics.fill(x0, y0, x1, y1, color);
                    }

                    @Override
                    public void text(Component text, int x, int y, int color) {
                        graphics.drawString(font, text, x, y, color);
                    }

                    @Override
                    public void centered(Component text, int x, int y, int color) {
                        graphics.drawString(font, text, x - font.width(text) / 2, y, color);
                    }
                },
                mouseX,
                mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return handleClick(mouseX, mouseY, button);
    }
    *///?}
}
