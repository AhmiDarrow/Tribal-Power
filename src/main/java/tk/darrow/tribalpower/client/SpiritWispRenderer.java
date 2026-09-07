package tk.darrow.tribalpower.client;

import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.SpiritWispEntity;

public class SpiritWispRenderer extends MobRenderer<SpiritWispEntity, MarchCreatureModel<SpiritWispEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/spirit_wisp.png");

    public SpiritWispRenderer(EntityRendererProvider.Context context) {
        super(context, new MarchCreatureModel<>(context.bakeLayer(MarchCreatureModel.WISP), true), 0.12F);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritWispEntity entity) {
        return TEXTURE;
    }
}
