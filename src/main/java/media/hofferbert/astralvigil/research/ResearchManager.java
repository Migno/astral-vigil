package media.hofferbert.astralvigil.research;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.compat.Keys;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Server-side research logic: scanning subjects, awarding knowledge, attunements and constellations. */
public final class ResearchManager {
    public static final String FIRST_SCAN = "research/first_scan";

    private ResearchManager() {}

    public static ResearchData get(ServerPlayer player) {
        return player.getData(AVAttachments.RESEARCH);
    }

    private static void set(ServerPlayer player, ResearchData data) {
        player.setData(AVAttachments.RESEARCH, data);
        player.syncData(AVAttachments.RESEARCH);
    }

    public static Set<Alignment> alignmentsOf(BlockState state) {
        Set<Alignment> out = EnumSet.noneOf(Alignment.class);
        for (Alignment a : Alignment.values()) {
            if (state.is(AlignmentTags.blocks(a))) {
                out.add(a);
            }
        }
        return out;
    }

    public static Set<Alignment> alignmentsOf(Entity entity) {
        Set<Alignment> out = EnumSet.noneOf(Alignment.class);
        for (Alignment a : Alignment.values()) {
            if (entity.getType().builtInRegistryHolder().is(AlignmentTags.entities(a))) {
                out.add(a);
            }
        }
        return out;
    }

    public static Set<Alignment> alignmentsOf(Holder<Biome> biome) {
        Set<Alignment> out = EnumSet.noneOf(Alignment.class);
        for (Alignment a : Alignment.values()) {
            if (biome.is(AlignmentTags.biomes(a))) {
                out.add(a);
            }
        }
        return out;
    }

    public static void scanBlock(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        String subject = "block/" + BuiltInRegistries.BLOCK.getKey(state.getBlock());
        scan(player, subject, state.getBlock().getName(), alignmentsOf(state), AVServerConfig.RESEARCH_POINTS_BLOCK.get());
    }

    public static void scanEntity(ServerPlayer player, Entity entity) {
        String subject = "entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        scan(player, subject, entity.getType().getDescription(), alignmentsOf(entity), AVServerConfig.RESEARCH_POINTS_ENTITY.get());
    }

    /** Scans the sky above the player: the current structure (if inside one) and otherwise the biome. */
    public static void scanSurroundings(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();

        for (Alignment a : Alignment.values()) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, AlignmentTags.structures(a));
            if (start.isValid()) {
                Structure structure = start.getStructure();
                // HolderLookup works the same on both targets (Registry vs RegistryLookup return types differ)
                var found = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements()
                        .filter(h -> h.value() == structure)
                        .findFirst();
                if (found.isPresent()) {
                    Holder.Reference<Structure> holder = found.get();
                    var key = java.util.Optional.of(holder.key());
                    Set<Alignment> aligns = EnumSet.noneOf(Alignment.class);
                    for (Alignment b : Alignment.values()) {
                        if (holder.is(AlignmentTags.structures(b))) {
                            aligns.add(b);
                        }
                    }
                    scan(
                            player,
                            "structure/" + Keys.id(key.get()),
                            Component.literal(Keys.id(key.get()).getPath().replace('_', ' ')),
                            aligns,
                            AVServerConfig.RESEARCH_POINTS_STRUCTURE.get());
                    return;
                }
            }
        }

        if (!level.canSeeSky(pos.above())) {
            return; // biomes are read from the sky
        }
        Holder<Biome> biome = level.getBiome(pos);
        biome.unwrapKey().ifPresent(key -> scan(
                player,
                "biome/" + Keys.id(key),
                Component.translatable("biome." + Keys.id(key).getNamespace() + "." + Keys.id(key).getPath()),
                alignmentsOf(biome),
                AVServerConfig.RESEARCH_POINTS_BIOME.get()));
    }

    static void scan(ServerPlayer player, String subject, Component name, Set<Alignment> alignments, int points) {
        ResearchData before = get(player);
        MutableComponent alignmentText = describe(alignments);

        if (before.hasScanned(subject)) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.scan.known", name.copy(), alignmentText)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        ResearchData after = before.withScan(subject, alignments, alignments.isEmpty() ? 0 : points);
        award(player, FIRST_SCAN);
        player.sendSystemMessage(Component.translatable("message.astralvigil.scan.new", name.copy(), alignmentText)
                .withStyle(ChatFormatting.AQUA));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.PLAYERS, 1.0f, 1.6f);
        scanResult(player, before, after, alignments);
    }

    /** Stores the new research and handles attunements / revealed constellations. */
    static void scanResult(ServerPlayer player, ResearchData before, ResearchData after, Set<Alignment> alignments) {
        set(player, after);
        int threshold = AVServerConfig.RESEARCH_ATTUNEMENT.get();
        for (Alignment a : alignments) {
            if (before.knowledge(a) < threshold && after.knowledge(a) >= threshold) {
                award(player, "research/attuned_" + a.id());
                player.sendSystemMessage(Component.translatable(
                                "message.astralvigil.attuned",
                                Component.translatable(a.translationKey()).withColor(a.color()))
                        .withStyle(ChatFormatting.GOLD));
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                Constellations.all().stream()
                        .filter(c -> !Constellations.available(c, before, threshold) && Constellations.available(c, after, threshold))
                        .forEach(c -> player.sendSystemMessage(Component.translatable(
                                        "message.astralvigil.constellation.revealed", Component.translatable(c.translationKey()))
                                .withStyle(ChatFormatting.LIGHT_PURPLE)));
            }
        }
    }

    /** Admin/debug: grants knowledge directly (handles attunement like a scan would). */
    public static void grant(ServerPlayer player, Alignment alignment, int points) {
        scanResult(player, get(player), get(player).withScan("admin/" + alignment.id(), Set.of(alignment), points), Set.of(alignment));
    }

    public static void reset(ServerPlayer player) {
        set(player, ResearchData.EMPTY);
    }

    /** Called when a client reports a solved constellation (validated against availability). */
    public static void solveConstellation(ServerPlayer player, String id) {
        Constellations.byId(id).ifPresent(c -> {
            ResearchData data = get(player);
            if (data.hasConstellation(id)
                    || !Constellations.available(c, data, AVServerConfig.RESEARCH_ATTUNEMENT.get())) {
                return;
            }
            set(player, data.withConstellation(id));
            award(player, c.advancementId());
            player.sendSystemMessage(Component.translatable(
                            "message.astralvigil.constellation.learned", Component.translatable(c.translationKey()))
                    .withStyle(ChatFormatting.GOLD));
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.2f);
        });
    }

    static MutableComponent describe(Set<Alignment> alignments) {
        if (alignments.isEmpty()) {
            return Component.translatable("alignment.astralvigil.none").withStyle(ChatFormatting.DARK_GRAY);
        }
        List<Alignment> list = new ArrayList<>(alignments);
        MutableComponent out = Component.empty();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            Alignment a = list.get(i);
            out.append(Component.translatable(a.translationKey()).withColor(a.color()));
        }
        return out;
    }

    static void award(ServerPlayer player, String advancementPath) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements().get(AstralVigil.id(advancementPath));
        if (holder != null) {
            player.getAdvancements().award(holder, "research");
        }
    }
}
