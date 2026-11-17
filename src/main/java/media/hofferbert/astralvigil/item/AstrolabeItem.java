package media.hofferbert.astralvigil.item;

import media.hofferbert.astralvigil.compat.Celestial;
import media.hofferbert.astralvigil.compat.PlayerCompat;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.starfall.Directions;
import media.hofferbert.astralvigil.starfall.StarfallManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import media.hofferbert.astralvigil.research.ResearchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if <26.1 {
/*import net.minecraft.world.InteractionResultHolder;
*///?}

/**
 * The Astrolabe reads the sky: starlight intensity, moon phase, sky access – and it hums towards recently fallen
 * stars. Hold use to scan what it is pointed at (a block or creature within the scan range, otherwise the sky - biome
 * or structure - above you); the scan completes after {@link #SCAN_TICKS}. Worn on the head it works like a lens: the
 * HUD names whatever you look at, its alignments and whether you have researched it ({@code client/AstrolabeOverlay}).
 */
//? if >=26.1 {
public class AstrolabeItem extends Item {
//?} else {
/*public class AstrolabeItem extends Item implements net.minecraft.world.item.Equipable {
*///?}
    static final String[] MOON_PHASES = {
        "full_moon", "waning_gibbous", "third_quarter", "waning_crescent",
        "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"
    };

    /** Ticks of holding use until a scan completes. */
    public static final int SCAN_TICKS = 24;

    public AstrolabeItem(Properties properties) {
        super(properties);
    }

    //? if <26.1 {
    /*@Override
    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return net.minecraft.world.entity.EquipmentSlot.HEAD;
    }
    *///?}

    /** Worn on the head or held in either hand. */
    public static boolean active(Player player) {
        return player.getMainHandItem().getItem() instanceof AstrolabeItem
                || player.getOffhandItem().getItem() instanceof AstrolabeItem
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem() instanceof AstrolabeItem;
    }

    /** What the Astrolabe points at: the nearest creature or block within the scan range (same on client and server). */
    public static net.minecraft.world.phys.HitResult target(Player player, float partialTick) {
        double range = AVServerConfig.RESEARCH_SCAN_RANGE.get();
        net.minecraft.world.phys.HitResult block = player.pick(range, partialTick, false);
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition(partialTick);
        double reach = block.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        net.minecraft.world.phys.Vec3 end = eye.add(player.getViewVector(partialTick).scale(reach));
        net.minecraft.world.phys.AABB box = player.getBoundingBox().expandTowards(player.getViewVector(partialTick).scale(reach)).inflate(1.0);
        net.minecraft.world.phys.EntityHitResult entity = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, box, e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable(), reach * reach);
        return entity != null ? entity : block;
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    *///?}

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return SCAN_TICKS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            scan(serverLevel, player, stack);
        }
        return stack;
    }

    /** Completes a scan: creature, block or - pointed at the open air - the sky reading plus biome/structure. */
    static void scan(ServerLevel level, ServerPlayer player, ItemStack stack) {
        net.minecraft.world.phys.HitResult hit = target(player, 1.0f);
        if (hit instanceof net.minecraft.world.phys.EntityHitResult e) {
            ResearchManager.scanEntity(player, e.getEntity());
        } else if (hit instanceof net.minecraft.world.phys.BlockHitResult b && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            ResearchManager.scanBlock(player, b.getBlockPos());
        } else {
            read(level, player, stack);
            ResearchManager.scanSurroundings(player);
        }
        PlayerCompat.cooldown(player, stack, AVServerConfig.ASTROLABE_COOLDOWN.get());
    }

    /** Server side: builds and sends the reading. */
    public static void read(ServerLevel level, Player player, ItemStack stack) {
        BlockPos pos = player.blockPosition();
        double starlight = Celestial.starlight(level, pos);
        int moon = Celestial.moonPhase(level, pos);
        boolean sky = level.canSeeSky(pos.above());

        Component reading = Component.translatable(
                        "message.astralvigil.astrolabe.reading",
                        Component.literal(Math.round(starlight * 100) + "%").withStyle(ChatFormatting.AQUA),
                        Component.translatable("moon.astralvigil." + MOON_PHASES[Math.floorMod(moon, 8)])
                                .withStyle(ChatFormatting.WHITE),
                        Component.translatable(sky ? "sky.astralvigil.open" : "sky.astralvigil.blocked")
                                .withStyle(sky ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.GOLD);
        player.sendSystemMessage(reading);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            int gaze = media.hofferbert.astralvigil.eclipse.GazeManager.get(sp);
            if (gaze > 0) {
                player.sendSystemMessage(Component.translatable("message.astralvigil.astrolabe.gaze", gaze)
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }

        Component tracking = StarfallManager.nearestRecent(level, pos, AVServerConfig.ASTROLABE_TRACK_RANGE.get())
                .map(star -> (Component) Component.translatable(
                        "message.astralvigil.astrolabe.starfall",
                        Component.literal(String.valueOf((int) Math.sqrt(star.distSqr(pos)))),
                        Component.translatable("direction.astralvigil." + Directions.cardinal(star.getX() - pos.getX(), star.getZ() - pos.getZ()))))
                .orElseGet(() -> Component.translatable("message.astralvigil.astrolabe.no_starfall"));
        PlayerCompat.actionBar(player, tracking.copy().withStyle(ChatFormatting.AQUA));

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
        PlayerCompat.cooldown(player, stack, AVServerConfig.ASTROLABE_COOLDOWN.get());
    }
}
