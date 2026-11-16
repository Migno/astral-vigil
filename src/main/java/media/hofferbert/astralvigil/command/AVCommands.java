package media.hofferbert.astralvigil.command;

import com.mojang.brigadier.CommandDispatcher;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.starfall.StarfallManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /astralvigil ...} – operator/debug commands (permission level 2). */
public final class AVCommands {
    private AVCommands() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> register(e.getDispatcher()));
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(AstralVigil.MOD_ID)
                //? if >=26.1 {
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                //?} else {
                /*.requires(src -> src.hasPermission(2))
                *///?}
                .then(Commands.literal("starfall")
                        .executes(ctx -> starfallNear(ctx.getSource()))
                        .then(Commands.literal("here").executes(ctx -> starfallHere(ctx.getSource())))));
    }

    private static int starfallNear(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        return StarfallManager.trigger(level, pos, level.getRandom())
                .map(target -> {
                    source.sendSuccess(
                            () -> Component.translatable(
                                    "commands.astralvigil.starfall.success",
                                    target.getX(),
                                    target.getY(),
                                    target.getZ()),
                            true);
                    return 1;
                })
                .orElseGet(() -> {
                    source.sendFailure(Component.translatable("commands.astralvigil.starfall.failed"));
                    return 0;
                });
    }

    private static int starfallHere(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        BlockPos surface = new BlockPos(
                pos.getX() + 6, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX() + 6, pos.getZ()), pos.getZ());
        StarfallManager.start(level, surface, level.getRandom());
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.astralvigil.starfall.success", surface.getX(), surface.getY(), surface.getZ()),
                true);
        return 1;
    }
}
