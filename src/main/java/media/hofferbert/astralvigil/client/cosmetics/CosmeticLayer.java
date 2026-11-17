package media.hofferbert.astralvigil.client.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.cosmetics.Cosmetic;
import media.hofferbert.astralvigil.cosmetics.Cosmetic.Slot;
import media.hofferbert.astralvigil.cosmetics.Loadout;
import media.hofferbert.astralvigil.registry.AVAttachments;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//? if >=26.1 {
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
//?} else {
/*import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
*///?}

/**
 * Renders the head (crowns, halo) and back (capes) developer cosmetics on players: a base pass with normal lighting
 * plus an emissive pass ({@code eyes} render type, full bright, gently pulsing) for gems, sun emblems and star fields.
 * Vanilla model layers only - works with every player renderer and shader pack, no GeckoLib emissives needed.
 */
public final class CosmeticLayer {
    static final int FULL_BRIGHT = 0xF000F0;
    private static final Map<Cosmetic, ModelLayerLocation> HEAD_LAYERS = new EnumMap<>(Cosmetic.class);

    static {
        for (Cosmetic c : Cosmetic.options(Slot.HEAD)) {
            HEAD_LAYERS.put(c, new ModelLayerLocation(AstralVigil.id("cosmetic_" + c.id()), "main"));
        }
    }

    private CosmeticLayer() {}

    public static Identifier texture(Cosmetic c) {
        return AstralVigil.id("textures/entity/cosmetics/" + c.id() + ".png");
    }

    /** Emissive mask; the starfield cape alternates two masks so its stars twinkle. */
    public static Identifier glow(Cosmetic c, float ageInTicks) {
        String suffix = c == Cosmetic.STARFIELD_CAPE && ((int) (ageInTicks / 12)) % 2 == 1 ? "_glow_b" : "_glow";
        return AstralVigil.id("textures/entity/cosmetics/" + c.id() + suffix + ".png");
    }

    /** ARGB tint for the emissive pass: a slow breathing pulse (offset per cosmetic so they never pulse in sync). */
    public static int pulse(Cosmetic c, float ageInTicks) {
        float f = 0.78f + 0.22f * Mth.sin(ageInTicks * 0.12f + c.ordinal());
        int v = Mth.clamp((int) (f * 255), 0, 255);
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    static void addHead(PartDefinition head, Cosmetic c) {
        switch (c) {
            case SOLAR_CROWN -> head.addOrReplaceChild("crown", CosmeticMeshes.solarCrown(), PartPose.ZERO);
            case ECLIPSE_CROWN -> head.addOrReplaceChild("crown", CosmeticMeshes.eclipseCrown(), PartPose.ZERO);
            case SERAPH_HALO -> {
                PartDefinition tilt = head.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offsetAndRotation(0f, -12.5f, 0.5f, -0.22f, 0f, 0f));
                CosmeticMeshes.haloRing(tilt.addOrReplaceChild("spin", CubeListBuilder.create(), PartPose.ZERO));
            }
            default -> throw new IllegalArgumentException(c.toString());
        }
    }

    /** Halo: slow spin and a gentle float above the head. No-op for crowns. */
    static void animate(ModelPart head, float ageInTicks) {
        if (head.hasChild("halo")) {
            ModelPart halo = head.getChild("halo");
            halo.y = -12.5f + Mth.sin(ageInTicks * 0.08f) * 0.6f;
            halo.getChild("spin").yRot = ageInTicks * 0.04f;
        }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> HEAD_LAYERS.forEach(
                (c, loc) -> e.registerLayerDefinition(loc, () -> headLayer(c))));
        modBus.addListener(CosmeticLayer::addLayers);
        //? if >=26.1 {
        modBus.addListener((RegisterRenderStateModifiersEvent e) -> e.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                Loadout loadout = avatar instanceof Player p ? p.getData(AVAttachments.COSMETICS) : Loadout.EMPTY;
                state.setRenderData(LOADOUT, loadout);
                if (loadout.get(Slot.BACK).isPresent()) {
                    state.showCape = false; // our cape replaces the vanilla/Mojang cape
                }
            }
        }));
        //?}
    }

    //? if >=26.1 {
    static final ContextKey<Loadout> LOADOUT = new ContextKey<>(AstralVigil.id("cosmetics"));

    static LayerDefinition headLayer(Cosmetic c) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot().clearRecursively();
        addHead(root.getChild("head"), c);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer == null) {
                continue;
            }
            Map<Cosmetic, HeadModel> heads = new EnumMap<>(Cosmetic.class);
            HEAD_LAYERS.forEach((c, loc) -> heads.put(c, new HeadModel(event.getEntityModels().bakeLayer(loc))));
            renderer.addLayer(new Layer(renderer, heads, new PlayerCapeModel(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_CAPE))));
        }
    }

    /** Head-following model: a PlayerModel with only the cosmetic under the head (like the vanilla cape model). */
    static final class HeadModel extends PlayerModel {
        HeadModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            animate(head, state.ageInTicks);
        }
    }

    static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private final Map<Cosmetic, HeadModel> heads;
        private final PlayerCapeModel cape;

        Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, Map<Cosmetic, HeadModel> heads, PlayerCapeModel cape) {
            super(parent);
            this.heads = heads;
            this.cape = cape;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            Loadout loadout = state.getRenderData(LOADOUT);
            if (state.isInvisible || loadout == null) {
                return;
            }
            Optional<Cosmetic> head = loadout.get(Slot.HEAD);
            if (head.isPresent()) {
                Cosmetic c = head.get();
                HeadModel model = heads.get(c);
                collector.submitModel(model, state, poseStack, RenderTypes.entityCutout(texture(c)), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
                collector.submitModel(model, state, poseStack, RenderTypes.eyes(glow(c, state.ageInTicks)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        pulse(c, state.ageInTicks), null, state.outlineColor, null);
            }
            Optional<Cosmetic> back = loadout.get(Slot.BACK);
            if (back.isPresent() && !state.chestEquipment.has(DataComponents.GLIDER)) {
                Cosmetic c = back.get();
                poseStack.pushPose();
                if (!state.chestEquipment.isEmpty()) {
                    poseStack.translate(0.0F, -0.053125F, 0.06875F);
                }
                collector.submitModel(cape, state, poseStack, RenderTypes.entityCutout(texture(c)), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
                collector.submitModel(cape, state, poseStack, RenderTypes.eyes(glow(c, state.ageInTicks)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        pulse(c, state.ageInTicks), null, state.outlineColor, null);
                poseStack.popPose();
            }
        }
    }
    //?} else {
    /*static LayerDefinition headLayer(Cosmetic c) {
        MeshDefinition mesh = new MeshDefinition();
        addHead(mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO), c);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                Map<Cosmetic, ModelPart> heads = new EnumMap<>(Cosmetic.class);
                HEAD_LAYERS.forEach((c, loc) -> heads.put(c, event.getEntityModels().bakeLayer(loc).getChild("head")));
                renderer.addLayer(new Layer(renderer, heads));
            }
        }
    }

    static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final Map<Cosmetic, ModelPart> heads;

        Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, Map<Cosmetic, ModelPart> heads) {
            super(parent);
            this.heads = heads;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (player.isInvisible()) {
                return;
            }
            Loadout loadout = player.getData(AVAttachments.COSMETICS);
            Optional<Cosmetic> head = loadout.get(Slot.HEAD);
            if (head.isPresent()) {
                Cosmetic c = head.get();
                ModelPart part = heads.get(c);
                part.copyFrom(getParentModel().head);
                animate(part, ageInTicks);
                part.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(c))), light, OverlayTexture.NO_OVERLAY);
                part.render(poseStack, buffers.getBuffer(RenderType.eyes(glow(c, ageInTicks))), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pulse(c, ageInTicks));
            }
            Optional<Cosmetic> back = loadout.get(Slot.BACK);
            if (back.isPresent() && !player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                Cosmetic c = back.get();
                poseStack.pushPose();
                capeTransform(poseStack, player, partialTick);
                getParentModel().renderCloak(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(c))), light, OverlayTexture.NO_OVERLAY);
                getParentModel().renderCloak(poseStack, buffers.getBuffer(RenderType.eyes(glow(c, ageInTicks))), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }

        // Same cloak physics as vanilla CapeLayer (1.21.1), so the cosmetic cape moves exactly like a Mojang cape.
        private static void capeTransform(PoseStack poseStack, AbstractClientPlayer p, float pt) {
            poseStack.translate(0.0F, 0.0F, 0.125F);
            double dx = Mth.lerp(pt, p.xCloakO, p.xCloak) - Mth.lerp(pt, p.xo, p.getX());
            double dy = Mth.lerp(pt, p.yCloakO, p.yCloak) - Mth.lerp(pt, p.yo, p.getY());
            double dz = Mth.lerp(pt, p.zCloakO, p.zCloak) - Mth.lerp(pt, p.zo, p.getZ());
            float body = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
            double sin = Mth.sin(body * Mth.DEG_TO_RAD);
            double cos = -Mth.cos(body * Mth.DEG_TO_RAD);
            float lift = Mth.clamp((float) dy * 10.0F, -6.0F, 32.0F);
            float swing = Mth.clamp((float) (dx * sin + dz * cos) * 100.0F, 0.0F, 150.0F);
            float lean = Mth.clamp((float) (dx * cos - dz * sin) * 100.0F, -20.0F, 20.0F);
            float bob = Mth.lerp(pt, p.oBob, p.bob);
            lift += Mth.sin(Mth.lerp(pt, p.walkDistO, p.walkDist) * 6.0F) * 32.0F * bob;
            if (p.isCrouching()) {
                lift += 25.0F;
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + swing / 2.0F + lift));
            poseStack.mulPose(Axis.ZP.rotationDegrees(lean / 2.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - lean / 2.0F));
        }
    }
    *///?}
}
