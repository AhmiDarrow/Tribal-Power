package tk.darrow.tribalpower.client.wildlife;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** One model class for all March wildlife; each kind animates its own parts (see GeneratedWildlifeLayers). */
public class WildlifeModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final String kind;

    public WildlifeModel(ModelPart root, String kind, boolean translucent) {
        super(translucent ? RenderType::entityTranslucent : RenderType::entityCutoutNoCull);
        this.root = root;
        this.kind = kind;
    }

    public static ModelLayerLocation layer(String kind) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tribalpower", kind), "main");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean wet = entity.isInWater();
        float phase = entity.getId() * 1.7F;
        switch (kind) {
            case "glimmerfin" -> root.getChild("tail").yRot = -(wet ? 0.45F : 0.9F) * Mth.sin((wet ? 0.6F : 1.4F) * age + phase);
            case "drift_bell" -> {
                // The bell pulses and the tentacles trail behind the beat.
                float beat = Mth.sin(age * 0.12F + phase);
                ModelPart bell = root.getChild("bell");
                bell.y += beat * 0.6F;
                bell.xScale = bell.zScale = 1 + beat * 0.06F;
                bell.yScale = 1 - beat * 0.05F;
                for (int i = 0; i < 6; i++) {
                    ModelPart t = root.getChild("tentacle" + i);
                    t.y += beat * 0.6F;
                    t.xRot = Mth.sin(age * 0.09F + phase + i) * 0.22F;
                    t.zRot = Mth.cos(age * 0.08F + phase + i * 1.3F) * 0.22F;
                }
            }
            case "veil_ray" -> {
                float flap = Mth.sin(age * 0.22F + phase) * (wet ? 0.45F : 0.15F);
                root.getChild("wing_l").zRot = flap;
                root.getChild("wing_r").zRot = -flap;
                root.getChild("tail").yRot = Mth.sin(age * 0.15F + phase) * 0.25F;
            }
            case "silt_eel" -> {
                float speed = wet ? 0.35F : 0.8F;
                root.getChild("head").yRot = Mth.sin(age * speed + phase) * 0.18F;
                ModelPart seg = root.getChild("seg1");
                for (int i = 1; i <= 4; i++) {
                    seg.yRot = Mth.sin(age * speed + phase - i * 0.9F) * 0.35F;
                    if (i < 4) seg = seg.getChild("seg" + (i + 1));
                }
            }
            case "loom_swift" -> {
                boolean gliding = entity.getDeltaMovement().y < -0.02 && Mth.sin(age * 0.05F + phase) > 0;
                float flap = gliding ? 0.1F : Mth.sin(age * 1.3F + phase) * 0.9F;
                root.getChild("wing_l").zRot = flap;
                root.getChild("wing_r").zRot = -flap;
                ModelPart body = root.getChild("body");
                body.xRot = (float) Mth.clamp(-entity.getDeltaMovement().y * 2, -0.5, 0.5);
                root.getChild("tail").xRot = body.xRot;
            }
            default -> {}
        }
    }
}
