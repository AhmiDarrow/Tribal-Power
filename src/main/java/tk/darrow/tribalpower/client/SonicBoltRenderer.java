package tk.darrow.tribalpower.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.song.SonicBolt;

/** The vanilla arrow mesh, pointed with the bolt. */
public class SonicBoltRenderer extends ArrowRenderer<SonicBolt> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public SonicBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SonicBolt entity) {
        return TEXTURE;
    }
}
