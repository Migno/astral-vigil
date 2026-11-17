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
 * stars. Used on a block or creature it scans its celestial alignment; used into the air it also scans the
 * surrounding biome or structure.
 */
public class AstrolabeItem extends Item {
    static final String[] MOON_PHASES = {
        "full_moon", "waning_gibbous", "third_quarter", "waning_crescent",
        "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"
    };

    public AstrolabeItem(Properties properties) {
        super(properties);
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            read(serverLevel, player, stack);
            if (player instanceof ServerPlayer sp) {
                ResearchManager.scanSurroundings(sp);
            }
        }
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            read(serverLevel, player, stack);
            if (player instanceof ServerPlayer sp) {
                ResearchManager.scanSurroundings(sp);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    *///?}

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            ResearchManager.scanBlock(player, context.getClickedPos());
            PlayerCompat.cooldown(player, context.getItemInHand(), AVServerConfig.ASTROLABE_COOLDOWN.get());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            ResearchManager.scanEntity(sp, target);
            PlayerCompat.cooldown(player, stack, AVServerConfig.ASTROLABE_COOLDOWN.get());
        }
        return InteractionResult.SUCCESS;
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
