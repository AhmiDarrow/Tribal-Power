package tk.darrow.tribalpower.client.wildlife;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import tk.darrow.tribalpower.client.GeneratedMarchLayers;

/**
 * One model class for all March wildlife. The bodies are generated rigs (tools/creature_gen: wild_roster.py,
 * wild_bodies.py, in GeneratedMarchLayers); each kind animates its own groups by name.
 */
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

    /** The generated rig for one kind, on its 256 texel sheet. */
    public static LayerDefinition create(String kind) {
        var mesh = GeneratedMarchLayers.mesh(kind);
        if (mesh == null) throw new IllegalArgumentException("No generated rig for wildlife: " + kind);
        return LayerDefinition.create(mesh, 256, 256);
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
            case "glimmerfin" -> {
                root.getChild("tail").yRot = -(wet ? 0.45F : 0.9F) * Mth.sin((wet ? 0.6F : 1.4F) * age + phase);
                float row = Mth.sin(age * 0.4F + phase) * 0.35F;
                root.getChild("flap0").zRot = -row;
                root.getChild("flap1").zRot = row;
            }
            case "drift_bell" -> {
                // The bell pulses, the clapper swings under it and the tentacles trail behind the beat.
                float beat = Mth.sin(age * 0.12F + phase);
                float wide = 1 + beat * 0.06F;
                ModelPart bell = root.getChild("body");
                bell.y += beat * 0.6F;
                bell.xScale = bell.zScale = wide;
                bell.yScale = 1 - beat * 0.05F;
                ModelPart clapper = root.getChild("clapper");
                clapper.y += beat * 0.6F;
                clapper.xRot = Mth.sin(age * 0.1F + phase) * 0.22F;
                clapper.zRot = Mth.cos(age * 0.08F + phase) * 0.18F;
                for (int i = 0; i < 8; i++) {
                    ModelPart t = root.getChild("tendril" + i);
                    t.y += beat * 0.6F;
                    t.x *= wide;
                    t.z *= wide;
                    t.xRot = Mth.sin(age * 0.09F + phase + i) * 0.22F;
                    t.zRot = Mth.cos(age * 0.08F + phase + i * 1.3F) * 0.22F;
                }
            }
            case "veil_ray" -> {
                float flap = Mth.sin(age * 0.22F + phase) * (wet ? 0.45F : 0.15F);
                root.getChild("flap0").zRot = -flap;
                root.getChild("flap1").zRot = flap;
                root.getChild("tail").yRot = Mth.sin(age * 0.15F + phase) * 0.25F;
            }
            case "silt_eel" -> {
                // A wave runs down the body, growing toward the tail. Each segment turns about its own joint, and
                // that joint is put back on the end of the segment before it, so the head and every segment stay
                // joined at every pose.
                float speed = wet ? 0.35F : 0.8F;
                float t = age * speed + phase;
                root.getChild("head").yRot = Mth.sin(t + 0.9F) * 0.18F;
                ModelPart prev = null;
                for (int i = 0; i < 6; i++) {
                    ModelPart seg = root.getChild("seg" + i);
                    if (prev != null) {
                        PartPose a = prev.getInitialPose(), b = seg.getInitialPose();
                        float dx = b.x - a.x, dz = b.z - a.z, c = Mth.cos(prev.yRot), s = Mth.sin(prev.yRot);
                        seg.x = prev.x + c * dx + s * dz;
                        seg.y = prev.y + (b.y - a.y);
                        seg.z = prev.z - s * dx + c * dz;
                    }
                    seg.yRot = Mth.sin(t - i * 0.9F) * (0.14F + 0.06F * i);
                    prev = seg;
                }
            }
            case "loom_swift" -> {
                boolean gliding = entity.getDeltaMovement().y < -0.02 && Mth.sin(age * 0.05F + phase) > 0;
                float flap = gliding ? 0.1F : Mth.sin(age * 1.3F + phase) * 0.9F;
                root.getChild("flap0").zRot = -flap;
                root.getChild("flap1").zRot = flap;
                // The whole bird pitches with its climb or dive, about the middle of its body.
                float tilt = (float) Mth.clamp(-entity.getDeltaMovement().y * 2, -0.5, 0.5);
                for (String name : new String[]{"body", "tail", "flap0", "flap1"}) pitch(root.getChild(name), tilt, 19F, 0F);
            }
            default -> {}
        }
    }

    /** Turns a part by angle about the x axis through (y, z), moving its pivot round that line too. */
    private static void pitch(ModelPart part, float angle, float cy, float cz) {
        float dy = part.y - cy, dz = part.z - cz, c = Mth.cos(angle), s = Mth.sin(angle);
        part.y = cy + c * dy - s * dz;
        part.z = cz + s * dy + c * dz;
        part.xRot += angle;
    }
}
