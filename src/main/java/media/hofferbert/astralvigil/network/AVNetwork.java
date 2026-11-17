package media.hofferbert.astralvigil.network;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.gear.GearManager;
import media.hofferbert.astralvigil.research.ResearchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Custom payloads. Research data itself is synced by the attachment system. */
public final class AVNetwork {
    private AVNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(AVNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(SolveConstellation.TYPE, SolveConstellation.CODEC, AVNetwork::handleSolve)
                .playToServer(GearAction.TYPE, GearAction.CODEC, AVNetwork::handleGear);
    }

    private static void handleSolve(SolveConstellation payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ResearchManager.solveConstellation(player, payload.id());
            }
        });
    }

    private static void handleGear(GearAction payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                if (payload.action() == GearAction.CYCLE_STANCE) {
                    GearManager.cycleStance(player);
                } else {
                    GearManager.useAbility(player);
                }
            }
        });
    }

    /** Client -> server: ability key or stance key pressed. */
    public record GearAction(int action) implements CustomPacketPayload {
        public static final int USE_ABILITY = 0;
        public static final int CYCLE_STANCE = 1;
        public static final Type<GearAction> TYPE = new Type<>(AstralVigil.id("gear_action"));
        public static final StreamCodec<FriendlyByteBuf, GearAction> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, GearAction::action, GearAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: "I connected this constellation correctly". Server re-validates availability. */
    public record SolveConstellation(String id) implements CustomPacketPayload {
        public static final Type<SolveConstellation> TYPE = new Type<>(AstralVigil.id("solve_constellation"));
        public static final StreamCodec<FriendlyByteBuf, SolveConstellation> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(64), SolveConstellation::id, SolveConstellation::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
