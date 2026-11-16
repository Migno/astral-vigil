package media.hofferbert.astralvigil.registry;

import java.util.function.Supplier;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.research.ResearchData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AVAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AstralVigil.MOD_ID);

    /** Player research: alignment knowledge, scanned subjects, solved constellations. Survives death, synced to owner. */
    public static final Supplier<AttachmentType<ResearchData>> RESEARCH = ATTACHMENTS.register(
            "research",
            () -> AttachmentType.builder(() -> ResearchData.EMPTY)
                    //? if >=26.1 {
                    .serialize(ResearchData.MAP_CODEC)
                    //?} else {
                    /*.serialize(ResearchData.MAP_CODEC.codec())
                    *///?}
                    .copyOnDeath()
                    .sync(AVAttachments::syncToOwner, ResearchData.STREAM_CODEC)
                    .build());

    private AVAttachments() {}

    /** Only the owner needs their research; skip players without a negotiated channel (GameTest mock players). */
    private static boolean syncToOwner(IAttachmentHolder holder, ServerPlayer player) {
        return holder == player && player.connection != null && player.connection.hasChannel(SyncAttachmentsPayload.TYPE);
    }
}
