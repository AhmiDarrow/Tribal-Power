package tk.darrow.tribalpower.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.SpiritWispEntity;

public class SpiritWispRenderer extends MobRenderer<SpiritWispEntity, MarchCreatureModel<SpiritWispEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/spirit_wisp.png");
    /** Its flames and motes, drawn unlit. */
    private static final net.minecraft.client.renderer.RenderType GLOW = net.minecraft.client.renderer.RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/spirit_wisp_glow.png"));

    public SpiritWispRenderer(EntityRendererProvider.Context context) {
        super(context, new MarchCreatureModel<>(context.bakeLayer(MarchCreatureModel.WISP), "spirit_wisp"), 0.12F);
        addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
            @Override public net.minecraft.client.renderer.RenderType renderType() { return GLOW; }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritWispEntity entity) {
        return TEXTURE;
    }

    /** Young ones are bred now; draw them at the half size their hitbox already has. */
    @Override
    protected void scale(SpiritWispEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        float s = entity.getAgeScale();
        pose.scale(s, s, s);
    }
}
