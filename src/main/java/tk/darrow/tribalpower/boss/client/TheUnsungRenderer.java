package tk.darrow.tribalpower.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.boss.TheUnsungEntity;

/** Renders the half-scale drum model at 2x with a glow layer on rune lines, eyes and the ring lacquer. */
public class TheUnsungRenderer extends MobRenderer<TheUnsungEntity, TheUnsungModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/the_unsung.png");
    // Emissive (not additive "eyes") so the violet runes stay violet: the base texture already carries the rune colour,
    // and additive blending on top of it blew the lines out to pink in the headless client showcase.
    private static final RenderType GLOW = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/the_unsung_glow.png"));

    public TheUnsungRenderer(EntityRendererProvider.Context context) {
        super(context, new TheUnsungModel(context.bakeLayer(TheUnsungModel.LAYER)), 1.5F);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TheUnsungEntity entity, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                getParentModel().renderToBuffer(poseStack, buffer.getBuffer(GLOW), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        });
    }

    @Override
    protected void scale(TheUnsungEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2F, 2F, 2F);
    }

    @Override public ResourceLocation getTextureLocation(TheUnsungEntity entity) { return TEXTURE; }
}
