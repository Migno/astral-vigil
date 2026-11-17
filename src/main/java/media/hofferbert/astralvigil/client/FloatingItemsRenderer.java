package media.hofferbert.astralvigil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import media.hofferbert.astralvigil.crafting.ItemHolder;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

/** Items slowly spinning and bobbing above pedestals, the altar and the forge. */
//? if >=26.1 {
public class FloatingItemsRenderer<T extends BlockEntity & ItemHolder>
        implements BlockEntityRenderer<T, FloatingItemsRenderer.State> {
    public static class State extends BlockEntityRenderState {
        public List<ItemStackRenderState> items = List.of();
        public float time;
        public float height;
    }

    private final ItemModelResolver resolver;

    public FloatingItemsRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            T be, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        List<ItemStackRenderState> list = new ArrayList<>();
        int seed = (int) be.getBlockPos().asLong();
        int i = 0;
        for (ItemStack stack : be.displayedItems()) {
            ItemStackRenderState s = new ItemStackRenderState();
            resolver.updateForTopItem(s, stack, ItemDisplayContext.GROUND, be.getLevel(), null, seed + i++);
            list.add(s);
        }
        state.items = list;
        state.height = be.displayHeight();
        state.time = be.getLevel() != null ? (be.getLevel().getGameTime() % 360000) + partialTicks : 0f;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int n = state.items.size();
        for (int i = 0; i < n; i++) {
            ItemStackRenderState item = state.items.get(i);
            if (item.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            place(poseStack, i, n, state.time, state.height);
            item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
//?} else {
/*public class FloatingItemsRenderer<T extends BlockEntity & ItemHolder> implements BlockEntityRenderer<T> {
    public FloatingItemsRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(T be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        List<ItemStack> items = be.displayedItems();
        float time = be.getLevel() != null ? (be.getLevel().getGameTime() % 360000) + partialTick : 0f;
        int n = items.size();
        for (int i = 0; i < n; i++) {
            poseStack.pushPose();
            place(poseStack, i, n, time, be.displayHeight());
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    items.get(i), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, poseStack, buffers, be.getLevel(),
                    (int) be.getBlockPos().asLong() + i);
            poseStack.popPose();
        }
    }
*///?}

    /** One item: centred; several: on a small ring. */
    private static void place(PoseStack poseStack, int index, int count, float time, float height) {
        double bob = Math.sin((time + index * 13) / 12.0) * 0.04;
        poseStack.translate(0.5, height + bob, 0.5);
        if (count > 1) {
            double angle = Math.PI * 2 * index / count + time / 80.0;
            poseStack.translate(Math.cos(angle) * 0.22, 0, Math.sin(angle) * 0.22);
            poseStack.scale(0.7f, 0.7f, 0.7f);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees((time * 2.0f + index * 40) % 360));
    }
}
