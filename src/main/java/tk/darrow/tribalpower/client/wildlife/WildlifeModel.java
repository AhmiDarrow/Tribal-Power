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
 * wild_bodies.py, in GeneratedMarchLayers); each kind animates its own groups, found by name once.
 */
public class WildlifeModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    /** Every part, root included, gathered once: getAllParts() built a fresh stream per creature per frame. */
    private final ModelPart[] all;
    private final String kind;
    // Parts found by name once, not by building "tendril"+i and friends for every creature every frame; null when absent.
    private final ModelPart head, body, tail, clapper, flap0, flap1;
    private final ModelPart[] tendrils = new ModelPart[8], segs = new ModelPart[6];
    /** The loom swift's parts that pitch together with its climb or dive. */
    private final ModelPart[] swift;

    public WildlifeModel(ModelPart root, String kind, boolean translucent) {
        super(translucent ? RenderType::entityTranslucent : RenderType::entityCutoutNoCull);
        this.root = root;
        all = root.getAllParts().toArray(ModelPart[]::new);
        this.kind = kind;
        head = part("head"); body = part("body"); tail = part("tail"); clapper = part("clapper");
        flap0 = part("flap0"); flap1 = part("flap1");
        for (int i = 0; i < 8; i++) tendrils[i] = part("tendril" + i);
        for (int i = 0; i < 6; i++) segs[i] = part("seg" + i);
        swift = new ModelPart[]{body, tail, flap0, flap1};
    }

    private ModelPart part(String name) { return root.hasChild(name) ? root.getChild(name) : null; }

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
        for (ModelPart part : all) part.resetPose();
        boolean wet = entity.isInWater();
        float phase = entity.getId() * 1.7F;
        switch (kind) {
            case "glimmerfin" -> {
                tail.yRot = -(wet ? 0.45F : 0.9F) * Mth.sin((wet ? 0.6F : 1.4F) * age + phase);
                float row = Mth.sin(age * 0.4F + phase) * 0.35F;
                flap0.zRot = -row;
                flap1.zRot = row;
            }
            case "drift_bell" -> {
                // The bell pulses, the clapper swings under it and the tentacles trail behind the beat.
                float beat = Mth.sin(age * 0.12F + phase);
                float wide = 1 + beat * 0.06F;
                body.y += beat * 0.6F;
                body.xScale = body.zScale = wide;
                body.yScale = 1 - beat * 0.05F;
                clapper.y += beat * 0.6F;
                clapper.xRot = Mth.sin(age * 0.1F + phase) * 0.22F;
                clapper.zRot = Mth.cos(age * 0.08F + phase) * 0.18F;
                for (int i = 0; i < 8; i++) {
                    ModelPart t = tendrils[i];
                    t.y += beat * 0.6F;
                    t.x *= wide;
                    t.z *= wide;
                    t.xRot = Mth.sin(age * 0.09F + phase + i) * 0.22F;
                    t.zRot = Mth.cos(age * 0.08F + phase + i * 1.3F) * 0.22F;
                }
            }
            case "veil_ray" -> {
                float flap = Mth.sin(age * 0.22F + phase) * (wet ? 0.45F : 0.15F);
                flap0.zRot = -flap;
                flap1.zRot = flap;
                tail.yRot = Mth.sin(age * 0.15F + phase) * 0.25F;
            }
            case "silt_eel" -> {
                // A wave runs down the body, growing toward the tail. Each segment turns about its own joint, and
                // that joint is put back on the end of the segment before it, so the head and every segment stay
                // joined at every pose.
                float speed = wet ? 0.35F : 0.8F;
                float t = age * speed + phase;
                head.yRot = Mth.sin(t + 0.9F) * 0.18F;
                ModelPart prev = null;
                for (int i = 0; i < 6; i++) {
                    ModelPart seg = segs[i];
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
                flap0.zRot = -flap;
                flap1.zRot = flap;
                // The whole bird pitches with its climb or dive, about the middle of its body.
                float tilt = (float) Mth.clamp(-entity.getDeltaMovement().y * 2, -0.5, 0.5);
                for (ModelPart part : swift) pitch(part, tilt, 19F, 0F);
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
