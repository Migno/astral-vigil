package media.hofferbert.astralvigil.cosmetics;

import java.util.Set;
import java.util.UUID;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server side of the developer cosmetics: who may wear them and applying a loadout chosen in the Cosmetics screen.
 * Access is decided on the server only (built-in team list, the {@code cosmetics.developers} server config list, or a
 * development environment) and mirrored to the client in {@link AVAttachments#COSMETIC_ACCESS} for the screen.
 */
public final class CosmeticsManager {
    /** Mojang account UUIDs of the Astral Vigil team (online-mode accounts). Owner: add yours here. */
    static final Set<UUID> TEAM = Set.of();

    private CosmeticsManager() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                refresh(player);
            }
        });
    }

    public static boolean isDeveloper(Player player) {
        if (AVServerConfig.SPEC.isLoaded() && !AVServerConfig.COSMETICS_ENABLED.get()) {
            return false;
        }
        //? if >=26.1 {
        boolean production = FMLEnvironment.isProduction();
        //?} else {
        /*boolean production = FMLEnvironment.production;
        *///?}
        if (!production || TEAM.contains(player.getUUID())) {
            return true;
        }
        return AVServerConfig.SPEC.isLoaded() && AVServerConfig.COSMETIC_DEVELOPERS.get().contains(player.getUUID().toString());
    }

    /** Re-evaluates access on login; someone who lost access also loses the loadout. */
    public static void refresh(ServerPlayer player) {
        boolean access = isDeveloper(player);
        player.setData(AVAttachments.COSMETIC_ACCESS.get(), access);
        if (!access && !player.getData(AVAttachments.COSMETICS.get()).equals(Loadout.EMPTY)) {
            player.setData(AVAttachments.COSMETICS.get(), Loadout.EMPTY);
        }
    }

    /** Applies a loadout requested by the client. Returns false (and tells the player) if they have no access. */
    public static boolean apply(ServerPlayer player, Loadout requested) {
        if (!isDeveloper(player)) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.cosmetics.locked"), true);
            return false;
        }
        Loadout loadout = requested.sanitized();
        player.setData(AVAttachments.COSMETICS.get(), loadout);
        AstralVigil.LOGGER.debug("{} wears cosmetics {}", player.getName().getString(), loadout);
        return true;
    }
}
