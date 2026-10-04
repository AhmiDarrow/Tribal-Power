package tk.darrow.tribalpower.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.MarchWalkerEntity;

public class MarchWalkerRenderer extends MobRenderer<MarchWalkerEntity, MarchCreatureModel<MarchWalkerEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/march_walker.png");
    /** The spirit-light in the tips of its tines, drawn unlit. */
    private static final net.minecraft.client.renderer.RenderType GLOW = net.minecraft.client.renderer.RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/march_walker_glow.png"));

    public MarchWalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new MarchCreatureModel<>(context.bakeLayer(MarchCreatureModel.WALKER), "march_walker"), 0.5F);
        addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
            @Override public net.minecraft.client.renderer.RenderType renderType() { return GLOW; }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(MarchWalkerEntity entity) {
        return TEXTURE;
    }

    /** Young ones are bred now; draw them at the half size their hitbox already has. */
    @Override
    protected void scale(MarchWalkerEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        float s = entity.getAgeScale();
        pose.scale(s, s, s);
    }
}
