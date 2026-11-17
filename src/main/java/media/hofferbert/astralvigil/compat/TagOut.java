package media.hofferbert.astralvigil.compat;

//? if >=26.2 {
/*import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

// 26.2 dropped the intrinsic-holder tag appenders (add(Block), add(Item)); tags take ResourceKeys only.
// Wraps the vanilla appender and adds the element overloads back so the tag providers stay version-neutral.
public final class TagOut<T> implements TagAppender<T> {
    private final TagAppender<T> out;
    private final Function<T, ResourceKey<T>> key;

    private TagOut(TagAppender<T> out, Function<T, ResourceKey<T>> key) {
        this.out = out;
        this.key = key;
    }

    public static TagOut<Block> blocks(TagAppender<Block> out) {
        return new TagOut<>(out, b -> b.builtInRegistryHolder().key());
    }

    public static TagOut<Item> items(TagAppender<Item> out) {
        return new TagOut<>(out, i -> i.builtInRegistryHolder().key());
    }

    public static TagOut<net.minecraft.world.entity.EntityType<?>> entities(TagAppender<net.minecraft.world.entity.EntityType<?>> out) {
        return new TagOut<>(out, e -> e.builtInRegistryHolder().key());
    }

    @SafeVarargs
    public final TagOut<T> add(T... elements) {
        Arrays.stream(elements).forEach(e -> out.add(key.apply(e)));
        return this;
    }

    @Override
    public TagOut<T> add(ResourceKey<T> element) {
        out.add(element);
        return this;
    }

    @Override
    public TagOut<T> addOptional(ResourceKey<T> element) {
        out.addOptional(element);
        return this;
    }

    @Override
    public TagOut<T> addTag(TagKey<T> tag) {
        out.addTag(tag);
        return this;
    }

    @Override
    public TagOut<T> addOptionalTag(TagKey<T> tag) {
        out.addOptionalTag(tag);
        return this;
    }

    @Override
    public TagOut<T> add(TagEntry entry) {
        out.add(entry);
        return this;
    }

    @Override
    public TagOut<T> replace(boolean value) {
        out.replace(value);
        return this;
    }

    @Override
    public TagOut<T> remove(ResourceKey<T> element) {
        out.remove(element);
        return this;
    }

    @Override
    public TagOut<T> remove(TagKey<T> tag) {
        out.remove(tag);
        return this;
    }
}
*///?} else {
/** Only used from 26.2 on (element-based tag appenders); older versions keep the vanilla intrinsic appenders. */
public final class TagOut {
    private TagOut() {}
}
//?}
