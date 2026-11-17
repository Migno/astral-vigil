package media.hofferbert.astralvigil.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.firmament.Watcher;
import media.hofferbert.astralvigil.firmament.WatcherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
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
 * Watcher renderer: the shared GeckoLib rig with the Watcher's own texture, drawn 1.4x larger, plus an emissive pass
 * of {@code <id>_glowmask.png} (eyes, core, halo, wing tips, orbiting shards) using the vanilla {@code eyes} render type,
 * which also works with shader packs and on macOS/Linux (unlike GeckoLib's own emissive shader).
 */
public final class WatcherRenderer {
    public static final float SCALE = 1.4f;

    private WatcherRenderer() {}

    static Identifier glow(Watcher w) {
        return AstralVigil.id("textures/entity/watcher/" + w.id() + "_glowmask.png");
    }

    static DefaultedEntityGeoModel<WatcherEntity> model(Watcher w) {
        return new DefaultedEntityGeoModel<WatcherEntity>(AstralVigil.id("watcher")).withAltTexture(AstralVigil.id("watcher/" + w.id()));
    }

    //? if >=26.1 {
    public static <R extends EntityRenderState & GeoRenderState> GeoEntityRenderer<WatcherEntity, R> create(EntityRendererProvider.Context ctx, Watcher w) {
        GeoEntityRenderer<WatcherEntity, R> renderer = new GeoEntityRenderer<>(ctx, model(w));
        renderer.withScale(SCALE);
        renderer.withRenderLayer(new TextureLayerGeoLayer<>(renderer, glow(w), RenderTypes::eyes));
        return renderer;
    }
    //?} else {
    /*public static GeoEntityRenderer<WatcherEntity> create(EntityRendererProvider.Context ctx, Watcher w) {
        GeoEntityRenderer<WatcherEntity> renderer = new GeoEntityRenderer<>(ctx, model(w));
        renderer.withScale(SCALE);
        Identifier glow = glow(w);
        renderer.addRenderLayer(new GeoRenderLayer<>(renderer) {
            @Override
            public void render(PoseStack poseStack, WatcherEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
                RenderType eyes = RenderType.eyes(glow);
                getRenderer().reRender(bakedModel, poseStack, buffers, animatable, eyes, buffers.getBuffer(eyes), partialTick,
                        0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        });
        return renderer;
    }
    *///?}
}
