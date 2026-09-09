package tk.darrow.tribalpower.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.boss.TheUnsungEntity;

/** Renders the half-scale drum model at 2x with a glow layer on rune lines, eyes and the ring lacquer. */
public class TheUnsungRenderer extends MobRenderer<TheUnsungEntity, TheUnsungModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/the_unsung.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/the_unsung_glow.png"));

    public TheUnsungRenderer(EntityRendererProvider.Context context) {
        super(context, new TheUnsungModel(context.bakeLayer(TheUnsungModel.LAYER)), 1.5F);
        addLayer(new EyesLayer<>(this) { @Override public RenderType renderType() { return GLOW; } });
    }

    @Override
    protected void scale(TheUnsungEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2F, 2F, 2F);
    }

    @Override public ResourceLocation getTextureLocation(TheUnsungEntity entity) { return TEXTURE; }
}
