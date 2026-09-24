package tk.darrow.tribalpower.guardian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.guardian.GuardianEntity;

/**
 * Draws a guardian from its generated rig, with its lit parts on an emissive layer drawn full-bright in their
 * own colour (the additive eyes layer washed a molten crack to cream), the way The Unsung is drawn.
 */
public class GuardianRenderer extends MobRenderer<GuardianEntity, GuardianModel> {
    private final ResourceLocation texture;

    public static ModelLayerLocation layer(Guardian guardian) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tribalpower", guardian.id), "main");
    }

    public GuardianRenderer(EntityRendererProvider.Context context, Guardian guardian) {
        super(context, new GuardianModel(context.bakeLayer(layer(guardian))), guardian.width * 0.5F);
        texture = ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/" + guardian.id + ".png");
        RenderType glow = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/" + guardian.id + "_glow.png"));
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, GuardianEntity entity, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                getParentModel().renderToBuffer(poseStack, buffer.getBuffer(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        });
    }

    @Override public ResourceLocation getTextureLocation(GuardianEntity entity) { return texture; }
}
