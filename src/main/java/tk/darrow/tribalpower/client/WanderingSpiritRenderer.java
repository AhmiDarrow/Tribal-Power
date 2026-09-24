package tk.darrow.tribalpower.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.event.WanderingSpiritEntity;

/** A wandering spirit is a wisp in warmer light. */
public class WanderingSpiritRenderer extends MobRenderer<WanderingSpiritEntity, MarchCreatureModel<WanderingSpiritEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "textures/entity/wandering_spirit.png");

    public WanderingSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new MarchCreatureModel<>(context.bakeLayer(MarchCreatureModel.WISP), true), 0.12F);
    }

    @Override public ResourceLocation getTextureLocation(WanderingSpiritEntity entity) { return TEXTURE; }
}
