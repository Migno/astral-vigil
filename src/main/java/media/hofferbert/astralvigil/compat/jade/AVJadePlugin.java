package media.hofferbert.astralvigil.compat.jade;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.block.StarlightDeviceBlock;
import media.hofferbert.astralvigil.starlight.Tint;
import media.hofferbert.astralvigil.starlight.network.Devices;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration (optional, compile-only dependency; Jade discovers this class through {@link WailaPlugin}). All data
 * comes from the client-synced block entity, so no server provider is needed.
 */
@WailaPlugin
public class AVJadePlugin implements IWailaPlugin {
    public static final Identifier STARLIGHT = AstralVigil.id("starlight");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(StarlightProvider.INSTANCE, StarlightDeviceBlock.class);
    }

    public enum StarlightProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof StarlightBlockEntity be)) {
                return;
            }
            tooltip.add(Component.translatable(
                            "jade.astralvigil.starlight", be.buffer().total(), be.buffer().capacity())
                    .withStyle(ChatFormatting.AQUA));
            for (Tint tint : Tint.values()) {
                int amount = be.buffer().get(tint);
                if (amount > 0 && tint != Tint.PURE) {
                    tooltip.add(Component.literal("  ")
                            .append(Component.translatable(tint.translationKey()).withColor(tint.argb() & 0xFFFFFF))
                            .append(": " + amount));
                }
            }
            if (be instanceof Devices.Collector collector) {
                tooltip.add(Component.translatable(
                                "jade.astralvigil.gathering", String.format(java.util.Locale.ROOT, "%.1f", collector.currentRate()))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (accessor.getBlock() instanceof StarlightDeviceBlock device
                    && device.kind() != StarlightDeviceBlock.Kind.RESERVOIR) {
                tooltip.add(Component.translatable(
                                "jade.astralvigil.aim",
                                Component.translatable("direction.astralvigil." + be.facing().getSerializedName()))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        @Override
        public Identifier getUid() {
            return STARLIGHT;
        }
    }
}
