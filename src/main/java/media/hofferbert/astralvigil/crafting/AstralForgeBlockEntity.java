package media.hofferbert.astralvigil.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import media.hofferbert.astralvigil.compat.DataIO;
import media.hofferbert.astralvigil.compat.RecipeCompat;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVBlockEntities;
import media.hofferbert.astralvigil.registry.AVRecipes;
import media.hofferbert.astralvigil.starlight.Tint;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Astral Forge: drop up to four items in (one each), feed it starlight of the recipe's tint and it forges by itself.
 * The result stays in the forge until taken out.
 */
public class AstralForgeBlockEntity extends StarlightBlockEntity implements ItemHolder {
    public static final int SLOTS = 4;

    private final List<ItemStack> items = new ArrayList<>();
    private String activeRecipe = "";
    private int progress;
    private int duration;

    public AstralForgeBlockEntity(BlockPos pos, BlockState state) {
        super(AVBlockEntities.ASTRAL_FORGE.get(), pos, state, 2000);
    }

    @Override
    protected int capacityFromConfig() {
        return AVServerConfig.FORGE_CAPACITY.get();
    }

    @Override
    protected List<Output> outputs() {
        return List.of();
    }

    @Override
    protected boolean accepts(Tint tint, Direction travel) {
        return true;
    }

    public List<ItemStack> items() {
        return items;
    }

    public boolean insert(ItemStack one) {
        if (items.size() >= SLOTS || one.isEmpty()) {
            return false;
        }
        items.add(one);
        activeRecipe = "";
        progress = 0;
        syncNow();
        return true;
    }

    public ItemStack takeLast() {
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = items.remove(items.size() - 1);
        activeRecipe = "";
        progress = 0;
        syncNow();
        return out;
    }

    public int progress() {
        return progress;
    }

    public int duration() {
        return duration;
    }

    public boolean isCrafting() {
        return !activeRecipe.isEmpty();
    }

    @Override
    public List<ItemStack> displayedItems() {
        return items;
    }

    @Override
    protected void produce(ServerLevel level) {
        if (items.isEmpty()) {
            return;
        }
        ItemsInput input = new ItemsInput(ItemStack.EMPTY, items);
        Optional<StarlightRecipe> recipe = activeRecipe.isEmpty()
                ? Optional.empty()
                : RecipeCompat.byId(level, activeRecipe).filter(r -> r.matches(input, level));
        if (recipe.isEmpty()) {
            Optional<RecipeCompat.Found> found = RecipeCompat.find(level, AVRecipes.FORGE.get(), input);
            if (found.isEmpty()) {
                if (!activeRecipe.isEmpty()) {
                    activeRecipe = "";
                    progress = 0;
                    syncNow();
                }
                return;
            }
            activeRecipe = found.get().id();
            duration = found.get().recipe().time();
            progress = 0;
            recipe = Optional.of(found.get().recipe());
        }
        StarlightRecipe r = recipe.get();
        int perSecond = (int) Math.ceil(r.starlight() * (double) WORK_INTERVAL / Math.max(1, r.time()));
        if (buffer.get(r.tint()) < perSecond) {
            syncNow();
            return;
        }
        buffer.extract(r.tint(), perSecond, false);
        progress += WORK_INTERVAL;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                worldPosition.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.05);
        if (progress >= r.time()) {
            items.clear();
            items.add(r.resultCopy());
            activeRecipe = "";
            progress = 0;
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.6f);
        }
        syncNow();
    }

    @Override
    protected void writeExtra(DataIO.Writer out) {
        out.putItems("items", items);
        out.putString("recipe", activeRecipe);
        out.putInt("progress", progress);
        out.putInt("duration", duration);
    }

    @Override
    protected void readExtra(DataIO.Reader in) {
        items.clear();
        items.addAll(in.getItems("items"));
        activeRecipe = in.getString("recipe", "");
        progress = in.getInt("progress", 0);
        duration = in.getInt("duration", 0);
    }
}
