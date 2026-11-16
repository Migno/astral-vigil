package media.hofferbert.astralvigil.network;

import media.hofferbert.astralvigil.AstralVigil;
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
        event.registrar("1").playToServer(SolveConstellation.TYPE, SolveConstellation.CODEC, AVNetwork::handleSolve);
    }

    private static void handleSolve(SolveConstellation payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ResearchManager.solveConstellation(player, payload.id());
            }
        });
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
