package media.hofferbert.astralvigil.firmament;

import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//? if >=26.1 {
import net.minecraft.world.level.portal.TeleportTransition;
//?} else {
/*import net.minecraft.world.level.portal.DimensionTransition;
*///?}

/** The Firmament dimension: travel via the Ascension Key, low gravity, one-time sanctum construction. */
public final class Firmament {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, AstralVigil.id("firmament"));
    public static final BlockPos LANDING = new BlockPos(0, 290, 0);
    static final Identifier GRAVITY_ID = AstralVigil.id("firmament_gravity");
    static final Identifier FALL_ID = AstralVigil.id("firmament_fall");
    static final String RETURN_KEY = "astralvigil_return";

    private Firmament() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Firmament::onPlayerTick);
    }

    public static boolean isFirmament(Level level) {
        return level.dimension() == LEVEL;
    }

    /** Ascension Key: up into the Firmament, or back to where the player ascended from. */
    public static void travel(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        if (isFirmament(player.level())) {
            CompoundTag back = player.getPersistentData().getCompound(RETURN_KEY)
                    //? if >=26.1 {
                    .orElse(new CompoundTag())
                    //?}
                    ;
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                    Identifier.tryParse(get(back, "dim", "minecraft:overworld")));
            ServerLevel target = server.getLevel(dim);
            if (target == null) {
                target = server.overworld();
            }
            Vec3 pos = new Vec3(getD(back, "x", 0), getD(back, "y", 100), getD(back, "z", 0));
            teleport(player, target, pos);
            return;
        }
        ServerLevel firmament = server.getLevel(LEVEL);
        if (firmament == null) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.firmament.missing").withStyle(ChatFormatting.RED));
            return;
        }
        CompoundTag back = new CompoundTag();
        back.putString("dim", player.level().dimension()
                //? if >=26.1 {
                .identifier()
                //?} else {
                /*.location()
                *///?}
                .toString());
        back.putDouble("x", player.getX());
        back.putDouble("y", player.getY());
        back.putDouble("z", player.getZ());
        player.getPersistentData().put(RETURN_KEY, back);
        SanctumBuilder.ensureBuilt(firmament);
        teleport(player, firmament, Vec3.atBottomCenterOf(LANDING.above()));
        player.sendSystemMessage(Component.translatable("message.astralvigil.firmament.arrive").withStyle(ChatFormatting.AQUA));
    }

    private static String get(CompoundTag tag, String key, String fallback) {
        //? if >=26.1 {
        return tag.getStringOr(key, fallback);
        //?} else {
        /*return tag.contains(key) ? tag.getString(key) : fallback;
        *///?}
    }

    private static double getD(CompoundTag tag, String key, double fallback) {
        //? if >=26.1 {
        return tag.getDoubleOr(key, fallback);
        //?} else {
        /*return tag.contains(key) ? tag.getDouble(key) : fallback;
        *///?}
    }

    static void teleport(ServerPlayer player, ServerLevel level, Vec3 pos) {
        //? if >=26.1 {
        player.teleport(new TeleportTransition(level, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
        //?} else {
        /*player.changeDimension(new DimensionTransition(level, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        *///?}
        level.playSound(null, BlockPos.containing(pos), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    /** Low gravity and softer landings while in the Firmament. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        boolean inside = isFirmament(player.level());
        apply(player.getAttribute(Attributes.GRAVITY), GRAVITY_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, inside);
        apply(player.getAttribute(Attributes.SAFE_FALL_DISTANCE), FALL_ID, 7.0, AttributeModifier.Operation.ADD_VALUE, inside);
        if (inside && player.getY() < -32) {
            // falling off the islands returns you to the landing instead of the void
            teleport(player, (ServerLevel) player.level(), Vec3.atBottomCenterOf(LANDING.above()));
            player.fallDistance = 0;
        }
    }

    private static void apply(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation op, boolean on) {
        if (attribute == null) {
            return;
        }
        boolean has = attribute.getModifier(id) != null;
        if (on && !has) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, op));
        } else if (!on && has) {
            attribute.removeModifier(id);
        }
    }
}
