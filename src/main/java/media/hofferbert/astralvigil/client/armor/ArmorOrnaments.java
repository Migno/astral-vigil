package media.hofferbert.astralvigil.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.armor.ArmorSet;
import media.hofferbert.astralvigil.armor.ArmorSets;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//? if >=26.1 {
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
//?} else {
/*import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
*///?}

/**
 * Renders the celestial armour beyond the flat vanilla layers, on players and armour stands:
 * <ul>
 *   <li>an emissive pass over the armour layer itself ({@code eyes} render type, gently pulsing) for the glowing seams;</li>
 *   <li>3D ornaments per worn piece (halos, crowns, wings, sun disc, mantle …) with a base and an emissive pass.</li>
 * </ul>
 * Vanilla model parts only (no GeckoLib emissives, see CLAUDE.md), stateless animation on the pose stack so the
 * deferred 26.1 renderer can share the baked parts. Seraph ornaments grow with the chestplate's awakening level.
 */
public final class ArmorOrnaments {
    static final int FULL_BRIGHT = 0xF000F0;

    /** The player/armour-stand bone an ornament follows. */
    public enum Bone {
        HEAD,
        BODY,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_LEG,
        LEFT_LEG
    }

    /** Stateless ornament animations (applied on the pose stack after the pivot). */
    public enum Anim {
        NONE,
        HALO,
        HALO_REVERSE,
        SPIN_Z,
        SPIN_Z_SLOW,
        FLAP_RIGHT,
        FLAP_LEFT,
        SWAY,
        BOB
    }

    /** One ornament as described by the generated {@link ArmorMeshes}. */
    public record Piece(String name, Bone bone, EquipmentSlot slot, int minLevel, Anim anim) {}

    record Baked(Piece piece, ModelPart pivot, ModelPart body) {}

    record SetModel(List<Baked> pieces, Identifier texture, Identifier glow, Identifier armorGlow, Identifier leggingsGlow) {
        Identifier layerGlow(EquipmentSlot slot) {
            return slot == EquipmentSlot.LEGS ? leggingsGlow : armorGlow;
        }
    }

    private ArmorOrnaments() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ArmorOrnaments::addLayers);
        //? if >=26.1 {
        // A back ornament (wings, sun disc, mantle) replaces the vanilla cape.
        modBus.addListener((RegisterRenderStateModifiersEvent e) -> e.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (ArmorSets.inSlot(avatar.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST) != null) {
                    state.showCape = false;
                }
            }
        }));
        //?}
    }

    static Map<ArmorSet, SetModel> bake() {
        Map<ArmorSet, SetModel> map = new EnumMap<>(ArmorSet.class);
        map.put(ArmorSet.SERAPH, bake(ArmorSet.SERAPH, ArmorMeshes.seraph(), ArmorMeshes.SERAPH_PIECES));
        map.put(ArmorSet.SOLAR, bake(ArmorSet.SOLAR, ArmorMeshes.solar(), ArmorMeshes.SOLAR_PIECES));
        map.put(ArmorSet.LUNAR, bake(ArmorSet.LUNAR, ArmorMeshes.lunar(), ArmorMeshes.LUNAR_PIECES));
        return map;
    }

    private static SetModel bake(ArmorSet set, LayerDefinition layer, List<Piece> pieces) {
        ModelPart root = layer.bakeRoot();
        List<Baked> baked = new ArrayList<>();
        for (Piece p : pieces) {
            ModelPart pivot = root.getChild(p.name());
            baked.add(new Baked(p, pivot, pivot.getChild("body")));
        }
        String id = set.id();
        return new SetModel(List.copyOf(baked),
                AstralVigil.id("textures/entity/armor/" + id + "_ornaments.png"),
                AstralVigil.id("textures/entity/armor/" + id + "_ornaments_glow.png"),
                AstralVigil.id("textures/entity/armor/" + id + "_glow.png"),
                AstralVigil.id("textures/entity/armor/" + id + "_leggings_glow.png"));
    }

    static ModelPart bone(HumanoidModel<?> model, Bone bone) {
        return switch (bone) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
        };
    }

    static void animate(Anim anim, PoseStack ps, float age, float walk) {
        switch (anim) {
            case HALO -> {
                ps.translate(0f, Mth.sin(age * 0.08f) * 0.6f / 16f, 0f);
                ps.mulPose(Axis.YP.rotation(age * 0.04f));
            }
            case HALO_REVERSE -> {
                ps.translate(0f, Mth.sin(age * 0.08f + 1.5f) * 0.6f / 16f, 0f);
                ps.mulPose(Axis.YP.rotation(-age * 0.07f));
            }
            case SPIN_Z -> ps.mulPose(Axis.ZP.rotation(age * 0.02f));
            case SPIN_Z_SLOW -> ps.mulPose(Axis.ZP.rotation(-age * 0.012f));
            case FLAP_RIGHT -> ps.mulPose(Axis.YP.rotation(Mth.sin(age * 0.09f) * 0.08f));
            case FLAP_LEFT -> ps.mulPose(Axis.YP.rotation(-Mth.sin(age * 0.09f) * 0.08f));
            case SWAY -> ps.mulPose(Axis.XP.rotation(0.05f + Math.min(1f, walk) * 0.55f + Mth.sin(age * 0.06f) * 0.03f));
            case BOB -> ps.translate(0f, Mth.sin(age * 0.1f) * 0.5f / 16f, 0f);
            case NONE -> {}
        }
    }

    /** ARGB tint for the emissive passes: a slow breathing pulse; Seraph burns brighter with every awakening. */
    static int pulse(ArmorSet set, int level, float age) {
        float base = switch (set) {
            case SERAPH -> 0.64f + 0.12f * Math.min(2, level);
            case SOLAR -> 0.82f;
            case LUNAR -> 0.72f;
        };
        float speed = set == ArmorSet.SOLAR ? 0.15f : 0.1f;
        float f = Mth.clamp(base + 0.16f * Mth.sin(age * speed + set.ordinal() * 2.1f), 0.35f, 1f);
        int v = (int) (f * 255);
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    /** Which set's piece sits in each armour slot (head → feet), null for none. */
    static ArmorSet[] worn(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        return new ArmorSet[] {
            ArmorSets.inSlot(head, EquipmentSlot.HEAD), ArmorSets.inSlot(chest, EquipmentSlot.CHEST),
            ArmorSets.inSlot(legs, EquipmentSlot.LEGS), ArmorSets.inSlot(feet, EquipmentSlot.FEET)
        };
    }

    static boolean any(ArmorSet[] worn) {
        for (ArmorSet s : worn) {
            if (s != null) {
                return true;
            }
        }
        return false;
    }

    //? if >=26.1 {
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        Map<ArmorSet, SetModel> sets = bake();
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer == null) {
                continue;
            }
            boolean slim = skin == PlayerModelType.SLIM;
            ArmorModelSet<PlayerModel> glow = ArmorModelSet.bake(
                    slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR, event.getEntityModels(), p -> new PlayerModel(p, slim));
            renderer.addLayer(new Layer<>(renderer, sets, glow));
        }
        EntityRenderer<?, ?> stand = event.getRenderer(EntityType.ARMOR_STAND);
        if (stand instanceof ArmorStandRenderer renderer) {
            renderer.addLayer(new Layer<>(renderer, sets,
                    ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, event.getEntityModels(), ArmorStandArmorModel::new)));
        }
    }

    static final class Layer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
        private final Map<ArmorSet, SetModel> sets;
        private final ArmorModelSet<? extends HumanoidModel<S>> glowModels;

        Layer(RenderLayerParent<S, M> parent, Map<ArmorSet, SetModel> sets, ArmorModelSet<? extends HumanoidModel<S>> glowModels) {
            super(parent);
            this.sets = sets;
            this.glowModels = glowModels;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
            if (state.isBaby
                    || (state instanceof ArmorStandRenderState stand && stand.isSmall)
                    || (state.isInvisible && state instanceof AvatarRenderState)) {
                return;
            }
            ArmorSet[] worn = worn(state.headEquipment, state.chestEquipment, state.legsEquipment, state.feetEquipment);
            if (!any(worn)) {
                return;
            }
            int level = ArmorSets.level(state.chestEquipment);
            M parent = getParentModel();
            for (int i = 0; i < worn.length; i++) {
                ArmorSet set = worn[i];
                if (set == null) {
                    continue;
                }
                EquipmentSlot slot = ArmorSets.SLOTS.get(i);
                SetModel model = sets.get(set);
                int color = pulse(set, level, state.ageInTicks);
                collector.submitModel(glowModels.get(slot), state, poseStack, RenderTypes.eyes(model.layerGlow(slot)), FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null);
                for (Baked b : model.pieces()) {
                    if (b.piece().slot() != slot || b.piece().minLevel() > level) {
                        continue;
                    }
                    poseStack.pushPose();
                    parent.root().translateAndRotate(poseStack);
                    bone(parent, b.piece().bone()).translateAndRotate(poseStack);
                    b.pivot().translateAndRotate(poseStack);
                    animate(b.piece().anim(), poseStack, state.ageInTicks, state.walkAnimationSpeed);
                    collector.submitModelPart(b.body(), poseStack, RenderTypes.entityCutout(model.texture()), light,
                            OverlayTexture.NO_OVERLAY, null, -1, null);
                    collector.submitModelPart(b.body(), poseStack, RenderTypes.eyes(model.glow()), FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY, null, color, null);
                    poseStack.popPose();
                }
            }
        }
    }
    //?} else {
    /*private static void addLayers(EntityRenderersEvent.AddLayers event) {
        Map<ArmorSet, SetModel> sets = bake();
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                renderer.addLayer(new Layer<>(renderer, sets,
                        new HumanoidArmorModel<>(event.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidArmorModel<>(event.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR))));
            }
        }
        EntityRenderer<?> stand = event.getRenderer(EntityType.ARMOR_STAND);
        if (stand instanceof ArmorStandRenderer renderer) {
            renderer.addLayer(new Layer<>(renderer, sets,
                    new ArmorStandArmorModel(event.getEntityModels().bakeLayer(ModelLayers.ARMOR_STAND_INNER_ARMOR)),
                    new ArmorStandArmorModel(event.getEntityModels().bakeLayer(ModelLayers.ARMOR_STAND_OUTER_ARMOR))));
        }
    }

    static final class Layer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
        private final Map<ArmorSet, SetModel> sets;
        private final HumanoidModel<T> inner;
        private final HumanoidModel<T> outer;

        Layer(RenderLayerParent<T, M> parent, Map<ArmorSet, SetModel> sets, HumanoidModel<T> inner, HumanoidModel<T> outer) {
            super(parent);
            this.sets = sets;
            this.inner = inner;
            this.outer = outer;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isBaby() || (entity instanceof Player && entity.isInvisible())) {
                return;
            }
            ArmorSet[] worn = worn(entity.getItemBySlot(EquipmentSlot.HEAD), entity.getItemBySlot(EquipmentSlot.CHEST),
                    entity.getItemBySlot(EquipmentSlot.LEGS), entity.getItemBySlot(EquipmentSlot.FEET));
            if (!any(worn)) {
                return;
            }
            int level = ArmorSets.level(entity.getItemBySlot(EquipmentSlot.CHEST));
            M parent = getParentModel();
            for (int i = 0; i < worn.length; i++) {
                ArmorSet set = worn[i];
                if (set == null) {
                    continue;
                }
                EquipmentSlot slot = ArmorSets.SLOTS.get(i);
                SetModel model = sets.get(set);
                int color = pulse(set, level, ageInTicks);
                HumanoidModel<T> armor = slot == EquipmentSlot.LEGS ? inner : outer;
                parent.copyPropertiesTo(armor);
                visibility(armor, slot);
                armor.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(model.layerGlow(slot))), FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, color);
                for (Baked b : model.pieces()) {
                    if (b.piece().slot() != slot || b.piece().minLevel() > level) {
                        continue;
                    }
                    poseStack.pushPose();
                    bone(parent, b.piece().bone()).translateAndRotate(poseStack);
                    b.pivot().translateAndRotate(poseStack);
                    animate(b.piece().anim(), poseStack, ageInTicks, limbSwingAmount);
                    b.body().render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture())), light, OverlayTexture.NO_OVERLAY);
                    b.body().render(poseStack, buffers.getBuffer(RenderType.eyes(model.glow())), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
                    poseStack.popPose();
                }
            }
        }

        // Same part visibility as the vanilla HumanoidArmorLayer.
        private static void visibility(HumanoidModel<?> model, EquipmentSlot slot) {
            model.setAllVisible(false);
            switch (slot) {
                case HEAD -> {
                    model.head.visible = true;
                    model.hat.visible = true;
                }
                case CHEST -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                case LEGS -> {
                    model.body.visible = true;
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
                default -> {
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
            }
        }
    }
    *///?}
}
