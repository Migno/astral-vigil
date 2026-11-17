package media.hofferbert.astralvigil.firmament;

import com.mojang.math.Transformation;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Animated floating blocks in the realms: vanilla block displays that bob and slowly turn. The server re-targets each
 * display's transformation every {@link #PERIOD} ticks and the client interpolates between the poses, so the motion
 * is smooth, costs a handful of packets and works with every renderer and shader pack.
 */
public final class FloatingBlocks {
    public static final String TAG = "astralvigil_float";
    static final int PERIOD = 40;

    private FloatingBlocks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FloatingBlocks::onLevelTick);
    }

    /** Spawns one floating block (scale 1 = a full block) centred on {@code at}. */
    public static void spawn(ServerLevel level, Vec3 at, BlockState state, float scale, boolean glow) {
        Display.BlockDisplay display = EntityType.BLOCK_DISPLAY.create(level
                //? if >=26.1 {
                , net.minecraft.world.entity.EntitySpawnReason.STRUCTURE
                //?}
                );
        if (display == null) {
            return;
        }
        display.setPos(at.x, at.y, at.z);
        display.setBlockState(state);
        display.addTag(TAG);
        display.addTag(TAG + "_" + Float.floatToIntBits(scale));
        display.setViewRange(4.0f);
        if (glow) {
            display.setBrightnessOverride(new Brightness(15, 15));
        }
        display.setTransformation(pose(display, scale, 0));
        level.addFreshEntity(display);
    }

    static java.util.Set<String> tags(net.minecraft.world.entity.Entity e) {
        //? if >=26.1 {
        return e.entityTags();
        //?} else {
        /*return e.getTags();
        *///?}
    }

    static float scaleOf(Display display) {
        for (String t : tags(display)) {
            if (t.startsWith(TAG + "_")) {
                return Float.intBitsToFloat(Integer.parseInt(t.substring(TAG.length() + 1)));
            }
        }
        return 1.0f;
    }

    /** Pure pose of a display at animation step {@code step}: bob up and down, slow spin, gentle tilt. */
    static Transformation pose(Display display, float scale, long step) {
        int seed = display.getUUID().hashCode();
        float phase = (seed & 0xFF) / 255.0f * Mth.TWO_PI;
        float t = step * 0.35f + phase;
        float bob = Mth.sin(t) * 0.35f * Math.max(0.6f, scale);
        float spin = (step * (8 + (seed >>> 8 & 7)) + (seed >>> 12 & 0xFF)) * Mth.DEG_TO_RAD * ((seed & 0x100) == 0 ? 1 : -1);
        float tilt = Mth.sin(t * 0.7f) * 0.18f;
        Quaternionf rot = new Quaternionf().rotateY(spin).rotateX(tilt).rotateZ(tilt * 0.6f);
        Vector3f half = new Vector3f(scale / 2, scale / 2, scale / 2).rotate(rot);
        Vector3f translation = new Vector3f(-half.x, -half.y + bob, -half.z);
        return new Transformation(translation, rot, new Vector3f(scale, scale, scale), null);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || Realm.of(level) == null) {
            return;
        }
        long time = level.getGameTime();
        if (time % PERIOD != 0) {
            return;
        }
        long step = time / PERIOD;
        List<? extends Display.BlockDisplay> displays = level.getEntities(EntityType.BLOCK_DISPLAY, d -> tags(d).contains(TAG));
        for (Display.BlockDisplay d : displays) {
            d.setTransformationInterpolationDelay(0);
            d.setTransformationInterpolationDuration(PERIOD);
            d.setTransformation(pose(d, scaleOf(d), step));
        }
    }
}
