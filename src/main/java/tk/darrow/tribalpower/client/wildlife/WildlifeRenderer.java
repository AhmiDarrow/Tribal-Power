package tk.darrow.tribalpower.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractFish;

/** Renders March wildlife: its texture, an emissive layer where the art has one, and a flop out of water. */
public class WildlifeRenderer<T extends Mob> extends MobRenderer<T, WildlifeModel<T>> {
    private final ResourceLocation texture;

    public WildlifeRenderer(EntityRendererProvider.Context context, String kind, float shadow, boolean translucent, boolean glows) {
        super(context, new WildlifeModel<>(context.bakeLayer(WildlifeModel.layer(kind)), kind, translucent), shadow);
        texture = ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/" + kind + ".png");
        if (glows) {
            RenderType glow = RenderType.eyes(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/" + kind + "_glow.png"));
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return glow;
                }
            });
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void setupRotations(T entity, PoseStack pose, float bob, float yaw, float partialTick, float scale) {
        super.setupRotations(entity, pose, bob, yaw, partialTick, scale);
        if (entity instanceof AbstractFish && !entity.isInWater()) {
            pose.translate(0.1F, 0.1F, -0.1F);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            pose.mulPose(Axis.YP.rotationDegrees(4.3F * Mth.sin(0.6F * bob)));
        }
    }
}
