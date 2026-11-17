package media.hofferbert.astralvigil.compat.jade;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.block.RitualBlock;
import media.hofferbert.astralvigil.block.StarlightDeviceBlock;
import media.hofferbert.astralvigil.crafting.AstralForgeBlockEntity;
import media.hofferbert.astralvigil.crafting.ConvergenceAltarBlockEntity;
import media.hofferbert.astralvigil.crafting.PedestalBlockEntity;
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
        registration.registerBlockComponent(StarlightProvider.INSTANCE, RitualBlock.class);
        registration.registerBlockComponent(RitualProvider.INSTANCE, RitualBlock.class);
    }

    public static final Identifier RITUAL = AstralVigil.id("ritual");

    /** Altar stability/progress, forge progress, held items. */
    public enum RitualProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof ConvergenceAltarBlockEntity altar) {
                int s = altar.stability();
                ChatFormatting color = s >= 100 ? ChatFormatting.GREEN : s >= 70 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                tooltip.add(Component.translatable("jade.astralvigil.stability", s).withStyle(color));
                if (altar.isCrafting() && altar.duration() > 0) {
                    tooltip.add(Component.translatable("jade.astralvigil.progress", 100 * altar.progress() / altar.duration()));
                }
                if (!altar.catalyst().isEmpty()) {
                    tooltip.add(Component.translatable("jade.astralvigil.holding", altar.catalyst().getHoverName()));
                }
            } else if (accessor.getBlockEntity() instanceof AstralForgeBlockEntity forge) {
                if (forge.isCrafting() && forge.duration() > 0) {
                    tooltip.add(Component.translatable("jade.astralvigil.progress", 100 * forge.progress() / forge.duration()));
                }
            } else if (accessor.getBlockEntity() instanceof PedestalBlockEntity pedestal && !pedestal.item().isEmpty()) {
                tooltip.add(Component.translatable("jade.astralvigil.holding", pedestal.item().getHoverName()));
            }
        }

        @Override
        public Identifier getUid() {
            return RITUAL;
        }
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
