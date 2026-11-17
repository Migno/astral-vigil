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
 * Access is decided on the server only - the built-in team list below, or a development environment - and mirrored to
 * the client in {@link AVAttachments#COSMETIC_ACCESS} for the screen. Server admins cannot grant access (only switch the
 * cosmetics off with {@code cosmetics.enabled}); new team members are added here, in code.
 */
public final class CosmeticsManager {
    /** Mojang account UUIDs of the Astral Vigil team (online-mode accounts). The only way to grant access. */
    static final Set<UUID> TEAM = Set.of(
            UUID.fromString("46c36850-3377-4b83-9804-ae2b572495c1")); // Migno

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
        return !production || TEAM.contains(player.getUUID());
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
