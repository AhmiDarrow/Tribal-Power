package tk.darrow.tribalpower.guardian.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.guardian.GuardianEntity;

/**
 * Animates a guardian by part name, the way the March roster is animated, plus the attack pose its ability
 * calls for: a slam drops the arms, a charge lowers the head, a swoop banks the wings, a sweep swings.
 * The bodies are generated rigs (tools/creature_gen: boss_roster.py, boss_bodies.py, in GeneratedMarchLayers),
 * whose parts are all flat root children; so the jaw is carried by the head's turn before it opens, and a
 * serpent's segments and tail are each put back on the end of the one before them every frame, so the body
 * stays joined at every pose.
 */
public class GuardianModel extends HierarchicalModel<GuardianEntity> {
    private final ModelPart root;
    /** Every part, root included, gathered once: getAllParts() built a fresh stream every frame. */
    private final ModelPart[] all;
    // Parts found by name once, not by building "leg"+i and friends for every guardian every frame; null when absent.
    private final ModelPart head, tail, jaw;
    private final ModelPart[] legs = new ModelPart[8], arms = new ModelPart[2], wings = new ModelPart[2],
            segs = new ModelPart[12], tendrils = new ModelPart[12];

    public GuardianModel(ModelPart root) {
        this.root = root;
        this.all = root.getAllParts().toArray(ModelPart[]::new);
        head = part("head");
        tail = part("tail");
        jaw = part("jaw");
        for (int i = 0; i < 8; i++) legs[i] = part("leg" + i);
        for (int i = 0; i < 2; i++) { arms[i] = part("arm" + i); wings[i] = part("wing" + i); }
        for (int i = 0; i < 12; i++) { segs[i] = part("seg" + i); tendrils[i] = part("tendril" + i); }
    }

    private ModelPart part(String name) { return root.hasChild(name) ? root.getChild(name) : null; }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(GuardianEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        for (ModelPart part : all) part.resetPose();
        Guardian guardian = entity.guardian();
        float attack = attackPose(entity, age);
        if (head != null) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD + (guardian.ability == Guardian.Ability.CHARGE ? attack * 0.6F : 0);
        }
        for (int i = 0; i < 8; i++) if (legs[i] != null) {
            legs[i].xRot = Mth.cos(swing * 0.6662F + (i % 2 == 0 ? Mth.PI : 0) + (i / 2) * 0.4F) * amount * 0.8F;
        }
        for (int i = 0; i < 2; i++) {
            ModelPart arm = arms[i];
            if (arm != null) {
                arm.xRot = Mth.cos(swing * 0.6662F + i * Mth.PI) * amount * 0.5F;
                switch (guardian.ability) {
                    case SLAM -> arm.xRot -= attack * 2.4F;
                    case SWEEP -> arm.yRot = (i == 0 ? -1 : 1) * attack * 1.8F;
                    case SNARE, TIDE -> arm.xRot -= attack * 1.2F;
                    default -> {}
                }
            }
            ModelPart wing = wings[i];
            if (wing != null) {
                float flap = Mth.sin(age * 0.5F) * 0.7F * (i == 0 ? 1 : -1);
                wing.zRot = guardian.flying ? flap - attack * 0.9F * (i == 0 ? 1 : -1) : Mth.sin(age * 0.2F) * 0.15F * (i == 0 ? 1 : -1);
            }
        }
        ModelPart prev = null;
        for (int i = 0; i < 12; i++) {
            if (segs[i] != null) {
                if (prev != null) ride(segs[i], prev);
                segs[i].yRot = Mth.sin(age * 0.14F - i * 0.5F) * 0.25F + Mth.sin(swing * 0.6F - i * 0.5F) * amount * 0.35F;
                prev = segs[i];
            }
            ModelPart t = tendrils[i];
            if (t != null) {
                t.xRot = Mth.cos(age * 0.1F + i * 0.7F) * 0.25F - attack * 0.8F;
                t.zRot = Mth.sin(age * 0.08F + i * 0.9F) * 0.25F;
            }
        }
        if (tail != null) {
            if (prev != null) ride(tail, prev); // a serpent's tail hangs on the end of its last segment
            tail.yRot = Mth.sin(age * 0.1F) * 0.2F + (prev != null ? prev.yRot : 0);
        }
        if (guardian.ability == Guardian.Ability.FROST && head != null) head.xRot += attack * 0.4F;
        if (jaw != null) {
            float open = attack * 0.5F + Mth.sin(age * 0.07F) * 0.05F;
            if (head != null) {
                ride(jaw, head);
                jaw.yRot = head.yRot;
                jaw.xRot = head.xRot + open;
            } else {
                jaw.xRot = open;
            }
        }
        if (guardian.flying) root.y = Mth.sin(age * 0.1F) * 1.6F;
        if (entity.isWarded()) root.y -= 1.5F;
        root.xScale = root.yScale = root.zScale = guardian.scale;
        root.y -= (guardian.scale - 1) * 24F;
    }

    /**
     * Puts a part's pivot where its parent's current turn (pitch, then yaw, as ModelPart applies them) carries
     * it about the parent's own pivot, from where the two sit in the rig, so the joint between them holds.
     */
    private static void ride(ModelPart child, ModelPart parent) {
        PartPose a = parent.getInitialPose(), b = child.getInitialPose();
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        float cx = Mth.cos(parent.xRot), sx = Mth.sin(parent.xRot);
        float y1 = cx * dy - sx * dz, z1 = sx * dy + cx * dz;
        float cy = Mth.cos(parent.yRot), sy = Mth.sin(parent.yRot);
        child.x = parent.x + cy * dx + sy * z1;
        child.y = parent.y + y1;
        child.z = parent.z - sy * dx + cy * z1;
    }

    /** 1 at the moment of an attack, easing back to 0 over half a second. */
    private static float attackPose(GuardianEntity entity, float age) {
        long now = entity.level().getGameTime();
        int at = entity.attackTime();
        if (at == 0) return 0;
        // int arithmetic on both sides, as the stamp was cut to an int, so a game time past 2^31 still lines up
        float since = ((int) now - at) + (age % 1F);
        return since < 0 || since > 12 ? 0 : 1 - since / 12F;
    }
}
