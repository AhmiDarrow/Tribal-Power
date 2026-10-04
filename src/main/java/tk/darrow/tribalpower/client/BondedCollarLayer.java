package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.LatticeAnimal;

/**
 * A tinted spirit-cord collar drawn over bonded animals; one overlay texture per species UV layout. Every gentle
 * March creature has one (tools/creature_gen paints it on that rig's own layout). A species without one draws no
 * collar rather than the missing-texture checkerboard over its whole body.
 */
public class BondedCollarLayer<T extends Mob> extends RenderLayer<T,LatticeCreatureModel<T>> {
    public static final int TINT=0xFF7EFFCB;
    private final ResourceLocation texture;
    /** Looked up once, on first use: resources are not loaded yet when renderers are built. */
    private Boolean present;
    public BondedCollarLayer(RenderLayerParent<T,LatticeCreatureModel<T>> parent,CreatureProfile profile) {
        super(parent);texture=ResourceLocation.fromNamespaceAndPath("tribalpower","textures/entity/bonded_collar_"+profile.id+".png");
    }
    @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,T entity,float swing,float amount,float partial,float age,float yaw,float pitch) {
        if(!(entity instanceof LatticeAnimal animal) || !animal.isBonded() || animal.isInvisible())return;
        if(present==null)present=net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        if(present)
            getParentModel().renderToBuffer(pose,buffer.getBuffer(RenderType.entityCutoutNoCull(texture)),light,OverlayTexture.NO_OVERLAY,TINT);
    }
}
