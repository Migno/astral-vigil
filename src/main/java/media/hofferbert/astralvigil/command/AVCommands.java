package media.hofferbert.astralvigil.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Arrays;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.ResearchManager;
import media.hofferbert.astralvigil.starlight.Tint;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
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
                        .then(Commands.literal("here").executes(ctx -> starfallHere(ctx.getSource()))))
                .then(Commands.literal("starlight")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("tint", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Tint.values()).map(Tint::id), b))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                .executes(ctx -> fillStarlight(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                                        StringArgumentType.getString(ctx, "tint"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("research")
                        .then(Commands.literal("reset").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ResearchManager.reset(player);
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.astralvigil.research.reset"), true);
                            return 1;
                        }))
                        .then(Commands.argument("alignment", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Alignment.values()).map(Alignment::id), b))
                                .then(Commands.argument("points", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Alignment a = Alignment.byId(StringArgumentType.getString(ctx, "alignment"))
                                                    .orElse(null);
                                            if (a == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown alignment"));
                                                return 0;
                                            }
                                            int points = IntegerArgumentType.getInteger(ctx, "points");
                                            ResearchManager.grant(player, a, points);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable(
                                                            "commands.astralvigil.research.granted",
                                                            points,
                                                            Component.translatable(a.translationKey())),
                                                    true);
                                            return 1;
                                        })))));
    }

    private static int fillStarlight(CommandSourceStack source, BlockPos pos, String tintId, int amount) {
        Tint tint = Arrays.stream(Tint.values()).filter(t -> t.id().equals(tintId)).findFirst().orElse(null);
        if (tint == null || !(source.getLevel().getBlockEntity(pos) instanceof StarlightBlockEntity be)) {
            source.sendFailure(Component.translatable("commands.astralvigil.starlight.failed"));
            return 0;
        }
        int stored = be.buffer().insert(tint, amount, false);
        be.syncNow();
        source.sendSuccess(() -> Component.translatable("commands.astralvigil.starlight.filled", stored), true);
        return stored;
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
