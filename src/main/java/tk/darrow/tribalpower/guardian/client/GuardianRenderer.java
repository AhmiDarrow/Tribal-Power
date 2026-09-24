package tk.darrow.tribalpower.guardian.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.guardian.GuardianEntity;

/** Draws a guardian from its generated rig, with its lit parts on an eyes layer. */
public class GuardianRenderer extends MobRenderer<GuardianEntity, GuardianModel> {
    private final ResourceLocation texture;

    public static ModelLayerLocation layer(Guardian guardian) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tribalpower", guardian.id), "main");
    }

    public GuardianRenderer(EntityRendererProvider.Context context, Guardian guardian) {
        super(context, new GuardianModel(context.bakeLayer(layer(guardian))), guardian.width * 0.5F);
        texture = ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/" + guardian.id + ".png");
        RenderType glow = RenderType.eyes(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/entity/" + guardian.id + "_glow.png"));
        addLayer(new EyesLayer<>(this) { @Override public RenderType renderType() { return glow; } });
    }

    @Override public ResourceLocation getTextureLocation(GuardianEntity entity) { return texture; }
}
