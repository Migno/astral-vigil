package media.hofferbert.astralvigil.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.compat.DataIO;
import media.hofferbert.astralvigil.compat.RecipeCompat;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVBlockEntities;
import media.hofferbert.astralvigil.registry.AVRecipes;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.research.AlignmentTags;
import media.hofferbert.astralvigil.starlight.Tint;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.world.entity.EntitySpawnReason;
//?} else {
/*import net.minecraft.world.entity.MobSpawnType;
*///?}

/**
 * Convergence Altar: catalyst on the altar, ingredients on surrounding pedestals, starlight beamed (or fed by a
 * touching reservoir) into the altar. Works once per second like every starlight device.
 *
 * <p>Stability = base − asymmetry penalty + aligned-block bonus (− Void blocks for non-Void recipes) − 10 without
 * open sky, clamped 0..100. Below 100, each second may cause an instability event: a pedestal item is flung away
 * (ritual aborts) or a hostile creature tears through.
 */
public class ConvergenceAltarBlockEntity extends StarlightBlockEntity implements ItemHolder {
    private ItemStack catalyst = ItemStack.EMPTY;
    private String activeRecipe = "";
    private int progress;
    private int duration;
    private int stability = 100;

    public ConvergenceAltarBlockEntity(BlockPos pos, BlockState state) {
        super(AVBlockEntities.CONVERGENCE_ALTAR.get(), pos, state, 4000);
    }

    // ------------------------------------------------------------------ device behaviour

    @Override
    protected int capacityFromConfig() {
        return AVServerConfig.ALTAR_CAPACITY.get();
    }

    @Override
    protected List<Output> outputs() {
        return List.of();
    }

    @Override
    protected boolean accepts(Tint tint, Direction travel) {
        return true;
    }

    // ------------------------------------------------------------------ items

    public ItemStack catalyst() {
        return catalyst;
    }

    public void setCatalyst(ItemStack stack) {
        catalyst = stack;
        syncNow();
    }

    public boolean isCrafting() {
        return !activeRecipe.isEmpty();
    }

    public int progress() {
        return progress;
    }

    public int duration() {
        return duration;
    }

    public int stability() {
        return stability;
    }

    @Override
    public List<ItemStack> displayedItems() {
        return catalyst.isEmpty() ? List.of() : List.of(catalyst);
    }

    @Override
    public float displayHeight() {
        return 1.05f;
    }

    // ------------------------------------------------------------------ pedestals + stability

    public List<PedestalBlockEntity> pedestals(ServerLevel level) {
        int r = AVServerConfig.ALTAR_RADIUS.get();
        List<PedestalBlockEntity> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-r, -1, -r), worldPosition.offset(r, 1, r))) {
            if (level.getBlockEntity(p) instanceof PedestalBlockEntity pedestal) {
                out.add(pedestal);
            }
        }
        return out;
    }

    /** Pure stability formula, see class doc. */
    public static int stability(int base, int asymmetric, int penalty, int aligned, int bonus, int maxBonus, int voidBlocks, boolean sky) {
        int value = base - asymmetric * penalty + Math.min(maxBonus, aligned * bonus) - voidBlocks * bonus - (sky ? 0 : 10);
        return Math.max(0, Math.min(100, value));
    }

    public int computeStability(ServerLevel level, Optional<Alignment> alignment) {
        List<PedestalBlockEntity> pedestals = pedestals(level).stream().filter(p -> !p.item().isEmpty()).toList();
        int asymmetric = 0;
        for (PedestalBlockEntity p : pedestals) {
            BlockPos mirrored = worldPosition.multiply(2).subtract(p.getBlockPos()).atY(p.getBlockPos().getY());
            if (!(level.getBlockEntity(mirrored) instanceof PedestalBlockEntity other) || other.item().isEmpty()) {
                asymmetric++;
            }
        }
        int aligned = 0;
        int voidBlocks = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-3, -1, -3), worldPosition.offset(3, 2, 3))) {
            BlockState s = level.getBlockState(p);
            if (alignment.isPresent() && s.is(AlignmentTags.blocks(alignment.get()))) {
                aligned++;
            } else if (alignment.map(a -> a != Alignment.VOID).orElse(true) && s.is(AlignmentTags.blocks(Alignment.VOID))) {
                voidBlocks++;
            }
        }
        return stability(
                AVServerConfig.STABILITY_BASE.get(),
                asymmetric,
                AVServerConfig.STABILITY_ASYMMETRY_PENALTY.get(),
                aligned,
                AVServerConfig.STABILITY_ALIGNED_BONUS.get(),
                AVServerConfig.STABILITY_ALIGNED_MAX.get(),
                voidBlocks,
                level.canSeeSky(worldPosition.above()));
    }

    ItemsInput input(ServerLevel level) {
        List<ItemStack> items = new ArrayList<>();
        for (PedestalBlockEntity p : pedestals(level)) {
            if (!p.item().isEmpty()) {
                items.add(p.item());
            }
        }
        return new ItemsInput(catalyst, items);
    }

    // ------------------------------------------------------------------ ritual

    /** Starts the ritual if the current layout matches a recipe. */
    public boolean tryStart(ServerLevel level) {
        if (isCrafting() || catalyst.isEmpty()) {
            return false;
        }
        Optional<RecipeCompat.Found> found = RecipeCompat.find(level, AVRecipes.CONVERGENCE.get(), input(level));
        if (found.isEmpty()) {
            return false;
        }
        activeRecipe = found.get().id();
        progress = 0;
        duration = found.get().recipe().time();
        stability = computeStability(level, found.get().recipe().alignment());
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.3f);
        level.players().stream()
                .filter(p -> p.blockPosition().closerThan(worldPosition, 8))
                .forEach(p -> media.hofferbert.astralvigil.eclipse.GazeManager.add(p, AVServerConfig.GAZE_PER_RITUAL.get()));
        syncNow();
        return true;
    }

    private void abort(ServerLevel level) {
        activeRecipe = "";
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.8f);
        syncNow();
    }

    @Override
    protected void produce(ServerLevel level) {
        if (!isCrafting()) {
            return;
        }
        Optional<StarlightRecipe> recipe = RecipeCompat.byId(level, activeRecipe);
        if (recipe.isEmpty() || !recipe.get().matches(input(level), level)) {
            abort(level);
            return;
        }
        StarlightRecipe r = recipe.get();
        stability = computeStability(level, r.alignment());

        int perSecond = r.time() <= 0 ? r.starlight() : (int) Math.ceil(r.starlight() * (double) WORK_INTERVAL / r.time());
        if (buffer.get(r.tint()) < perSecond) {
            // starved: the ritual waits for light
            level.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                    worldPosition.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
            syncNow();
            return;
        }
        buffer.extract(r.tint(), perSecond, false);
        progress += WORK_INTERVAL;
        streamParticles(level);

        if (stability < 100 && level.getRandom().nextDouble() < (100 - stability) / 100.0 * AVServerConfig.INSTABILITY_EVENT_FACTOR.get()) {
            instabilityEvent(level);
            if (!isCrafting()) {
                return;
            }
        }

        if (progress >= r.time()) {
            finish(level, r);
        } else {
            syncNow();
        }
    }

    private void finish(ServerLevel level, StarlightRecipe r) {
        for (PedestalBlockEntity p : pedestals(level)) {
            if (!p.item().isEmpty()) {
                //? if >=26.1 {
                var template = p.item().getItem().getCraftingRemainder();
                ItemStack remainder = template == null ? ItemStack.EMPTY : template.create();
                //?} else {
                /*ItemStack remainder = p.item().getCraftingRemainingItem();
                *///?}
                p.setItem(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
            }
        }
        catalyst = r.resultCopy();
        activeRecipe = "";
        progress = 0;
        Vec3 c = Vec3.atCenterOf(worldPosition).add(0, 0.8, 0);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.4, 0.4, 0.4, 0.08);
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.5f);
        syncNow();
    }

    private void streamParticles(ServerLevel level) {
        Vec3 target = Vec3.atCenterOf(worldPosition).add(0, 0.7, 0);
        for (PedestalBlockEntity p : pedestals(level)) {
            if (p.item().isEmpty()) {
                continue;
            }
            Vec3 from = Vec3.atCenterOf(p.getBlockPos()).add(0, 0.8, 0);
            for (int i = 1; i <= 4; i++) {
                Vec3 at = from.lerp(target, i / 5.0);
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    private void instabilityEvent(ServerLevel level) {
        List<PedestalBlockEntity> loaded = pedestals(level).stream().filter(p -> !p.item().isEmpty()).toList();
        if (!loaded.isEmpty() && level.getRandom().nextBoolean()) {
            PedestalBlockEntity victim = loaded.get(level.getRandom().nextInt(loaded.size()));
            ItemStack flung = victim.item();
            victim.setItem(ItemStack.EMPTY);
            Vec3 from = Vec3.atCenterOf(victim.getBlockPos()).add(0, 1, 0);
            ItemEntity entity = new ItemEntity(level, from.x, from.y, from.z, flung);
            entity.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.8, 0.5, (level.getRandom().nextDouble() - 0.5) * 0.8);
            level.addFreshEntity(entity);
            level.playSound(null, victim.getBlockPos(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.6f);
            abort(level);
            return;
        }
        List<? extends String> mobs = AVServerConfig.INSTABILITY_MOBS.get();
        if (mobs.isEmpty()) {
            return;
        }
        Identifier id = Identifier.tryParse(mobs.get(level.getRandom().nextInt(mobs.size())));
        if (id == null) {
            return;
        }
        BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> spawnHostile(level, type));
    }

    private void spawnHostile(ServerLevel level, EntityType<?> type) {
        //? if >=26.1 {
        Entity e = type.create(level, EntitySpawnReason.EVENT);
        //?} else {
        /*Entity e = type.create(level);
        *///?}
        if (e == null) {
            AstralVigil.LOGGER.warn("Instability mob {} could not be created", type);
            return;
        }
        e.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5);
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY(), e.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void writeExtra(DataIO.Writer out) {
        out.putItems("catalyst", List.of(catalyst));
        out.putString("recipe", activeRecipe);
        out.putInt("progress", progress);
        out.putInt("duration", duration);
        out.putInt("stability", stability);
    }

    @Override
    protected void readExtra(DataIO.Reader in) {
        List<ItemStack> items = in.getItems("catalyst");
        catalyst = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        activeRecipe = in.getString("recipe", "");
        progress = in.getInt("progress", 0);
        duration = in.getInt("duration", 0);
        stability = in.getInt("stability", 100);
    }
}
