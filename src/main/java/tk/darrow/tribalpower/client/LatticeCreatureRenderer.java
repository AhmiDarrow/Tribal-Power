package tk.darrow.tribalpower.client;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import tk.darrow.tribalpower.entity.CreatureProfile;
public class LatticeCreatureRenderer<T extends Mob> extends MobRenderer<T,LatticeCreatureModel<T>> {
    private final ResourceLocation texture;
    public static ModelLayerLocation layer(CreatureProfile p) {return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tribalpower",p.id),"main");}
    public LatticeCreatureRenderer(EntityRendererProvider.Context context,CreatureProfile profile) {
        super(context,new LatticeCreatureModel<>(context.bakeLayer(layer(profile))),profile.width*.45F);
        texture=ResourceLocation.fromNamespaceAndPath("tribalpower","textures/entity/"+profile.id+".png");
        var glow=RenderType.eyes(ResourceLocation.fromNamespaceAndPath("tribalpower","textures/entity/"+profile.id+"_glow.png"));
        addLayer(new EyesLayer<T,LatticeCreatureModel<T>>(this) { @Override public RenderType renderType() { return glow; } });
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return texture; }
}
