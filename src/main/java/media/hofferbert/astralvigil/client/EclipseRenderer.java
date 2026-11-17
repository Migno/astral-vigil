package media.hofferbert.astralvigil.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.eclipse.EclipseEcho;
import media.hofferbert.astralvigil.eclipse.EclipseEntity;
import media.hofferbert.astralvigil.eclipse.UmbralPylon;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.geckolib.cache.object.BakedGeoModel;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
*///?}

/**
 * Renderers for the Eclipse (drawn 1.4x, no vanilla death tip-over â€“ it has its own disintegration animation) and its
 * Umbral Pylons. Both get an emissive pass of {@code <name>_glowmask.png} with the vanilla {@code eyes} render type,
 * like the Watchers (works with shader packs and on macOS/Linux).
 */
public final class EclipseRenderer {
    public static final float ECLIPSE_SCALE = EclipseEntity.MODEL_SCALE;

    private EclipseRenderer() {}

    private static Identifier glow(String name) {
        return AstralVigil.id("textures/entity/eclipse/" + name + "_glowmask.png");
    }

    private static <T extends Entity & GeoAnimatable> DefaultedEntityGeoModel<T> model(String name) {
        return new DefaultedEntityGeoModel<T>(AstralVigil.id(name)).withAltTexture(AstralVigil.id("eclipse/" + name));
    }

    //? if >=26.1 {
    public static <R extends EntityRenderState & GeoRenderState> GeoEntityRenderer<EclipseEntity, R> eclipse(EntityRendererProvider.Context ctx) {
        GeoEntityRenderer<EclipseEntity, R> renderer = new GeoEntityRenderer<>(ctx, EclipseRenderer.<EclipseEntity>model("eclipse")) {
            @Override
            protected float getDeathMaxRotation(GeoRenderState renderState) {
                return 0f;
            }
        };
        renderer.withScale(ECLIPSE_SCALE);
        renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, glow("eclipse"), RenderTypes::eyes));
        return renderer;
    }

    public static <R extends EntityRenderState & GeoRenderState> GeoEntityRenderer<UmbralPylon, R> pylon(EntityRendererProvider.Context ctx) {
        GeoEntityRenderer<UmbralPylon, R> renderer = new GeoEntityRenderer<>(ctx, EclipseRenderer.<UmbralPylon>model("umbral_pylon"));
        renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, glow("umbral_pylon"), RenderTypes::eyes));
        return renderer;
    }

    /** The Eclipse Echo keeps its texture at the default path; its glowmask lives with the Eclipse's. */
    public static <R extends EntityRenderState & GeoRenderState> GeoEntityRenderer<EclipseEcho, R> echo(EntityRendererProvider.Context ctx) {
        GeoEntityRenderer<EclipseEcho, R> renderer = new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<EclipseEcho>(AstralVigil.id("eclipse_echo")));
        renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, glow("eclipse_echo"), RenderTypes::eyes));
        return renderer;
    }
    //?} else {
    /*public static GeoEntityRenderer<EclipseEntity> eclipse(EntityRendererProvider.Context ctx) {
        GeoEntityRenderer<EclipseEntity> renderer = new GeoEntityRenderer<>(ctx, EclipseRenderer.<EclipseEntity>model("eclipse")) {
            @Override
            protected float getDeathMaxRotation(EclipseEntity animatable) {
                return 0f;
            }
        };
        renderer.withScale(ECLIPSE_SCALE);
        renderer.addRenderLayer(glowLayer(renderer, glow("eclipse")));
        return renderer;
    }

    public static GeoEntityRenderer<UmbralPylon> pylon(EntityRendererProvider.Context ctx) {
        GeoEntityRenderer<UmbralPylon> renderer = new GeoEntityRenderer<>(ctx, EclipseRenderer.<UmbralPylon>model("umbral_pylon"));
        renderer.addRenderLayer(glowLayer(renderer, glow("umbral_pylon")));
        return renderer;
    }

    public static GeoEntityRenderer<EclipseEcho> echo(EntityRendererProvider.Context ctx) {
        GeoEntityRenderer<EclipseEcho> renderer = new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<EclipseEcho>(AstralVigil.id("eclipse_echo")));
        renderer.addRenderLayer(glowLayer(renderer, glow("eclipse_echo")));
        return renderer;
    }

    private static <T extends Entity & GeoAnimatable> GeoRenderLayer<T> glowLayer(GeoEntityRenderer<T> renderer, Identifier glow) {
        return new GeoRenderLayer<>(renderer) {
            @Override
            public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
                RenderType eyes = RenderType.eyes(glow);
                getRenderer().reRender(bakedModel, poseStack, buffers, animatable, eyes, buffers.getBuffer(eyes), partialTick,
                        0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        };
    }
    *///?}
}
