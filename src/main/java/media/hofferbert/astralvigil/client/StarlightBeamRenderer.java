package media.hofferbert.astralvigil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import media.hofferbert.astralvigil.config.AVClientConfig;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}

/**
 * Renders starlight beams with the vanilla beacon beam (rotated onto the beam axis, thinner, tinted). Purely
 * client-side; beam data comes from the device's synced block entity.
 */
//? if >=26.1 {
public class StarlightBeamRenderer
        implements BlockEntityRenderer<StarlightBlockEntity, StarlightBeamRenderer.State> {
    public static class State extends BlockEntityRenderState {
        public List<StarlightBlockEntity.Beam> beams = List.of();
        public float animationTime;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            StarlightBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.beams = be.beams();
        state.animationTime = be.getLevel() != null ? Math.floorMod(be.getLevel().getGameTime(), 40) + partialTicks : 0f;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (StarlightBlockEntity.Beam beam : state.beams) {
            float length = beam.lengthTenths() / 10f;
            if (length <= 0.05f) {
                continue;
            }
            int height = (int) Math.ceil(length);
            poseStack.pushPose();
            orient(poseStack, beam.direction(), length / height);
            BeaconRenderer.submitBeaconBeam(
                    poseStack, collector, BeaconRenderer.BEAM_LOCATION, 1.0f, state.animationTime, 0, height,
                    beam.tint().argb(), radius(), radius() * 1.6f);
            poseStack.popPose();
        }
    }
//?} else {
/*public class StarlightBeamRenderer implements BlockEntityRenderer<StarlightBlockEntity> {
    @Override
    public void render(
            StarlightBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        for (StarlightBlockEntity.Beam beam : be.beams()) {
            float length = beam.lengthTenths() / 10f;
            if (length <= 0.05f) {
                continue;
            }
            int height = (int) Math.ceil(length);
            poseStack.pushPose();
            orient(poseStack, beam.direction(), length / height);
            BeaconRenderer.renderBeaconBeam(
                    poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0f, gameTime, 0, height,
                    beam.tint().argb(), radius(), radius() * 1.6f);
            poseStack.popPose();
        }
    }
*///?}

    /** Rotates +Y onto {@code dir} around the block centre and squashes to the exact length. */
    private static void orient(PoseStack poseStack, Direction dir, float lengthScale) {
        poseStack.translate(0.5, 0.5, 0.5);
        switch (dir) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            default -> {}
        }
        poseStack.scale(1f, lengthScale, 1f);
        poseStack.translate(-0.5, 0, -0.5); // the beacon helper translates back by (0.5, 0, 0.5)
    }

    private static float radius() {
        return switch (AVClientConfig.SPEC.isLoaded() ? AVClientConfig.BEAM_QUALITY.get() : AVClientConfig.BeamQuality.HIGH) {
            case LOW -> 0.06f;
            case MEDIUM -> 0.07f;
            case HIGH -> 0.08f;
        };
    }

    //? if >=26.1 {
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
    //?} else {
    /*@Override
    public boolean shouldRenderOffScreen(StarlightBlockEntity be) {
        return true;
    }
    *///?}

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(StarlightBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(32);
    }
}
