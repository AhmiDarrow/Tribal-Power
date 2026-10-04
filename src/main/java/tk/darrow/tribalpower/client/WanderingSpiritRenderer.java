package tk.darrow.tribalpower.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.event.WanderingSpiritEntity;

/** A wandering spirit is a paper-lantern ghost carrying a warm light; its lit paper and candle are drawn unlit. */
public class WanderingSpiritRenderer extends MobRenderer<WanderingSpiritEntity, MarchCreatureModel<WanderingSpiritEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/wandering_spirit.png");
    private static final net.minecraft.client.renderer.RenderType GLOW = net.minecraft.client.renderer.RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/wandering_spirit_glow.png"));

    public WanderingSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new MarchCreatureModel<>(context.bakeLayer(MarchCreatureModel.WANDERER), "wandering_spirit"), 0.12F);
        addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
            @Override public net.minecraft.client.renderer.RenderType renderType() { return GLOW; }
        });
    }

    @Override public ResourceLocation getTextureLocation(WanderingSpiritEntity entity) { return TEXTURE; }
}
