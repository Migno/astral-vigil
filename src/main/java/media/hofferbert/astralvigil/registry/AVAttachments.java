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

    /** Eclipse Gaze (0..max). Survives death; synced to the owner for the Astrolabe reading. */
    public static final Supplier<AttachmentType<Integer>> GAZE = ATTACHMENTS.register(
            "gaze",
            () -> AttachmentType.builder(() -> 0)
                    //? if >=26.1 {
                    .serialize(com.mojang.serialization.Codec.INT.fieldOf("gaze"))
                    //?} else {
                    /*.serialize(com.mojang.serialization.Codec.INT)
                    *///?}
                    .copyOnDeath()
                    .sync(AVAttachments::syncToOwner, net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build());

    /** Watcher favour (watcher id -> favour). Survives death; synced to the owner. */
    public static final Supplier<AttachmentType<java.util.Map<String, Integer>>> FAVOUR = ATTACHMENTS.register(
            "favour",
            () -> AttachmentType.<java.util.Map<String, Integer>>builder(() -> java.util.Map.of())
                    //? if >=26.1 {
                    .serialize(com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, com.mojang.serialization.Codec.INT).fieldOf("favour"))
                    //?} else {
                    /*.serialize(com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, com.mojang.serialization.Codec.INT))
                    *///?}
                    .copyOnDeath()
                    .sync(AVAttachments::syncToOwner, net.minecraft.network.codec.ByteBufCodecs.map(
                            java.util.HashMap::new, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
                            net.minecraft.network.codec.ByteBufCodecs.VAR_INT).map(java.util.Map::copyOf, java.util.HashMap::new))
                    .build());

    /** Seraph Wings cosmetic: 0 unbound, 1 hidden, 2 shown. Survives death; synced to every client (it is visible). */
    public static final Supplier<AttachmentType<Integer>> WINGS = ATTACHMENTS.register(
            "wings",
            () -> AttachmentType.builder(() -> 0)
                    //? if >=26.1 {
                    .serialize(com.mojang.serialization.Codec.INT.fieldOf("wings"))
                    //?} else {
                    /*.serialize(com.mojang.serialization.Codec.INT)
                    *///?}
                    .copyOnDeath()
                    .sync((holder, to) -> to.connection != null && to.connection.hasChannel(SyncAttachmentsPayload.TYPE),
                            net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build());

    /** Developer cosmetics the player wears. Survives death; synced to every client (it is visible). */
    public static final Supplier<AttachmentType<media.hofferbert.astralvigil.cosmetics.Loadout>> COSMETICS = ATTACHMENTS.register(
            "cosmetics",
            () -> AttachmentType.builder(() -> media.hofferbert.astralvigil.cosmetics.Loadout.EMPTY)
                    //? if >=26.1 {
                    .serialize(media.hofferbert.astralvigil.cosmetics.Loadout.MAP_CODEC)
                    //?} else {
                    /*.serialize(media.hofferbert.astralvigil.cosmetics.Loadout.MAP_CODEC.codec())
                    *///?}
                    .copyOnDeath()
                    .sync((holder, to) -> to.connection != null && to.connection.hasChannel(SyncAttachmentsPayload.TYPE),
                            media.hofferbert.astralvigil.cosmetics.Loadout.STREAM_CODEC)
                    .build());

    /** Whether the server lets this player wear developer cosmetics (decided at login). Synced to the owner only. */
    public static final Supplier<AttachmentType<Boolean>> COSMETIC_ACCESS = ATTACHMENTS.register(
            "cosmetic_access",
            () -> AttachmentType.builder(() -> false)
                    //? if >=26.1 {
                    .serialize(com.mojang.serialization.Codec.BOOL.fieldOf("access"))
                    //?} else {
                    /*.serialize(com.mojang.serialization.Codec.BOOL)
                    *///?}
                    .copyOnDeath()
                    .sync(AVAttachments::syncToOwner, net.minecraft.network.codec.ByteBufCodecs.BOOL)
                    .build());

    private AVAttachments() {}

    /** Only the owner needs their research; skip players without a negotiated channel (GameTest mock players). */
    private static boolean syncToOwner(IAttachmentHolder holder, ServerPlayer player) {
        return holder == player && player.connection != null && player.connection.hasChannel(SyncAttachmentsPayload.TYPE);
    }
}
