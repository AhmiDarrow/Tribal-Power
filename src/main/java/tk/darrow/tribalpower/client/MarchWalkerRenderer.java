package tk.darrow.tribalpower.client;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.MarchWalkerEntity;

public class MarchWalkerRenderer extends MobRenderer<MarchWalkerEntity, PigModel<MarchWalkerEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/march_walker.png");

    public MarchWalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MarchWalkerEntity entity) {
        return TEXTURE;
    }
}
