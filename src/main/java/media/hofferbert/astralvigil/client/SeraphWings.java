package media.hofferbert.astralvigil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.item.SeraphFeatherItem;
import media.hofferbert.astralvigil.registry.AVAttachments;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//? if >=26.1 {
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.world.entity.Avatar;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
//?} else {
/*import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
*///?}

/**
 * Seraph Wings cosmetic: a plain vanilla model layer on the player body (rig from art/bbmodel/entity/seraph_wings,
 * converted to a {@code LayerDefinition}). Kept vanilla instead of GeckoLib so it works with every player renderer,
 * shader pack and skin layer mod; it rests angled back and spreads while falling.
 */
public final class SeraphWings {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(AstralVigil.id("seraph_wings"), "main");
    static final Identifier TEXTURE = AstralVigil.id("textures/entity/seraph_wings.png");

    private SeraphWings() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(LAYER, SeraphWings::layer));
        modBus.addListener(SeraphWings::addLayers);
        //? if >=26.1 {
        modBus.addListener((RegisterRenderStateModifiersEvent e) -> e.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                boolean player = avatar instanceof Player;
                state.setRenderData(SHOWN, player && shown((Player) avatar));
                state.setRenderData(GLIDING, player && gliding((Player) avatar));
                state.setRenderData(COSMETIC, player ? cosmetic((Player) avatar) : null);
            }
        }));
        //?}
    }

    static boolean shown(Player player) {
        return player.getData(AVAttachments.WINGS) == SeraphFeatherItem.SHOWN || cosmetic(player) != null;
    }

    /** Developer wing cosmetic (radiant/eclipse recolour with an emissive pass), or null for the plain Seraph Wings. */
    static media.hofferbert.astralvigil.cosmetics.Cosmetic cosmetic(Player player) {
        return player.getData(AVAttachments.COSMETICS).get(media.hofferbert.astralvigil.cosmetics.Cosmetic.Slot.WINGS).orElse(null);
    }

    static Identifier texture(media.hofferbert.astralvigil.cosmetics.Cosmetic cosmetic) {
        return cosmetic == null ? TEXTURE : media.hofferbert.astralvigil.client.cosmetics.CosmeticLayer.texture(cosmetic);
    }

    static boolean gliding(Player player) {
        return !player.onGround() && player.getDeltaMovement().y < -0.08 && !player.getAbilities().flying;
    }

    /** Spread angle (radians, positive = swept back) and flap for the current frame. */
    static float sweep(float ageInTicks, boolean gliding) {
        return gliding
                ? Mth.DEG_TO_RAD * (5f + 4f * Mth.sin(ageInTicks * 0.6f))
                : Mth.DEG_TO_RAD * (22.5f + 2.5f * Mth.sin(ageInTicks * (float) (Math.PI * 2 / 60)));
    }

    static void addWings(PartDefinition body) {
        body.addOrReplaceChild("wing_right", CubeListBuilder.create()
                        .texOffs(44, 0).addBox(-8f, -3f, 0f, 9f, 6f, 1f)
                        .texOffs(0, 0).addBox(-11f, 2f, 1f, 10f, 8f, 1f)
                        .texOffs(20, 9).addBox(-11f, 9f, 2f, 4f, 4f, 1f),
                PartPose.offset(-1.5f, 1.5f, 2.5f));
        body.addOrReplaceChild("wing_left", CubeListBuilder.create()
                        .texOffs(0, 9).addBox(-1f, -3f, 0f, 9f, 6f, 1f)
                        .texOffs(22, 0).addBox(1f, 2f, 1f, 10f, 8f, 1f)
                        .texOffs(30, 9).addBox(7f, 9f, 2f, 4f, 4f, 1f),
                PartPose.offset(1.5f, 1.5f, 2.5f));
    }

    /** Wings sit high on the shoulder blades, slightly enlarged, tips raised and swept back. */
    static void pose(ModelPart right, ModelPart left, float sweep) {
        for (ModelPart wing : new ModelPart[] {right, left}) {
            wing.xScale = 1.4f;
            wing.yScale = 1.4f;
            wing.zScale = 1.0f;
        }
        right.yRot = sweep;
        left.yRot = -sweep;
        right.zRot = 0.35f;
        left.zRot = -0.35f;
    }

    //? if >=26.1 {
    static final ContextKey<Boolean> SHOWN = new ContextKey<>(AstralVigil.id("wings_shown"));
    static final ContextKey<Boolean> GLIDING = new ContextKey<>(AstralVigil.id("wings_gliding"));
    static final ContextKey<media.hofferbert.astralvigil.cosmetics.Cosmetic> COSMETIC = new ContextKey<>(AstralVigil.id("wings_cosmetic"));

    static LayerDefinition layer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot().clearRecursively();
        addWings(root.getChild("body"));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new Layer(renderer, new Model(event.getEntityModels().bakeLayer(LAYER))));
            }
        }
    }

    /** Body-following wing model: a PlayerModel with only the two wings (like the vanilla cape model). */
    static final class Model extends PlayerModel {
        private final ModelPart right = body.getChild("wing_right");
        private final ModelPart left = body.getChild("wing_left");

        Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            pose(right, left, sweep(state.ageInTicks, Boolean.TRUE.equals(state.getRenderData(GLIDING))));
        }
    }

    static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private final Model model;

        Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, Model model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            if (state.isInvisible || !Boolean.TRUE.equals(state.getRenderData(SHOWN))) {
                return;
            }
            var cosmetic = state.getRenderData(COSMETIC);
            collector.submitModel(model, state, poseStack, RenderTypes.entityCutout(texture(cosmetic)), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
            if (cosmetic != null) {
                collector.submitModel(model, state, poseStack, RenderTypes.eyes(media.hofferbert.astralvigil.client.cosmetics.CosmeticLayer.glow(cosmetic, state.ageInTicks)),
                        0xF000F0, OverlayTexture.NO_OVERLAY, media.hofferbert.astralvigil.client.cosmetics.CosmeticLayer.pulse(cosmetic, state.ageInTicks),
                        null, state.outlineColor, null);
            }
        }
    }
    //?} else {
    /*static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        addWings(body);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new Layer(renderer, event.getEntityModels().bakeLayer(LAYER)));
            }
        }
    }

    static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final ModelPart body;
        private final ModelPart right;
        private final ModelPart left;

        Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
            super(parent);
            this.body = root.getChild("body");
            this.right = body.getChild("wing_right");
            this.left = body.getChild("wing_left");
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (player.isInvisible() || !shown(player)) {
                return;
            }
            body.copyFrom(getParentModel().body);
            pose(right, left, sweep(ageInTicks, gliding(player)));
            var cosmetic = cosmetic(player);
            body.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(cosmetic))), light, OverlayTexture.NO_OVERLAY);
            if (cosmetic != null) {
                body.render(poseStack, buffers.getBuffer(RenderType.eyes(media.hofferbert.astralvigil.client.cosmetics.CosmeticLayer.glow(cosmetic, ageInTicks))),
                        0xF000F0, OverlayTexture.NO_OVERLAY, media.hofferbert.astralvigil.client.cosmetics.CosmeticLayer.pulse(cosmetic, ageInTicks));
            }
        }
    }
    *///?}
}
