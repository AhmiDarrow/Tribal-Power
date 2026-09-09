package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import tk.darrow.tribalpower.tribe.KinRole;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribalKinEntity;

import java.util.EnumMap;
import java.util.Map;

/**
 * Base 256x256 skin per role (painted by tools/art/creatures.py through the Blender pipeline) plus a hood/cloak
 * overlay carrying the tribe glyph, tinted with the tribe colour.
 */
public class TribalKinRenderer extends MobRenderer<TribalKinEntity, TribalKinModel> {
    private static final Map<KinRole, ResourceLocation> SKINS = new EnumMap<>(KinRole.class);
    private static final Map<TribeDefinition, ResourceLocation> CLOAKS = new EnumMap<>(TribeDefinition.class);
    private static final ResourceLocation CLOAK = ResourceLocation.parse("tribalpower:textures/entity/kin_cloak.png");
    static {
        for (KinRole role : KinRole.values())
            SKINS.put(role, ResourceLocation.parse("tribalpower:textures/entity/kin_" + role.id() + ".png"));
        for (TribeDefinition tribe : TribeDefinition.values())
            CLOAKS.put(tribe, ResourceLocation.parse("tribalpower:textures/entity/kin_cloak_" + tribe.id() + ".png"));
    }

    public TribalKinRenderer(EntityRendererProvider.Context context) {
        super(context, new TribalKinModel(context.bakeLayer(TribalKinModel.LAYER)), 0.4F);
        addLayer(new CloakLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(TribalKinEntity entity) { return SKINS.get(entity.role()); }

    /** Renders the same model again with the cloak texture, coloured by the tribe. */
    public static final class CloakLayer extends RenderLayer<TribalKinEntity, TribalKinModel> {
        public CloakLayer(TribalKinRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, TribalKinEntity entity, float limbSwing, float limbAmount,
                           float partial, float age, float yaw, float pitch) {
            if (entity.isInvisible()) return;
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(CLOAKS.getOrDefault(entity.tribe(), CLOAK)));
            int colour = 0xFF000000 | entity.tribe().colour();
            getParentModel().renderToBuffer(pose, consumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0), colour);
        }
    }
}
