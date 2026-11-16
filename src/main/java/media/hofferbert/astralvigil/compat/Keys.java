package media.hofferbert.astralvigil.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** {@code ResourceKey#location()} was renamed to {@code identifier()} in 1.21.11. */
public final class Keys {
    private Keys() {}

    public static Identifier id(ResourceKey<?> key) {
        //? if >=26.1 {
        return key.identifier();
        //?} else {
        /*return key.location();
        *///?}
    }
}
