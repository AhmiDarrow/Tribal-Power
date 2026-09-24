package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.WeaponKind;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Each Spiritgear weapon swings the way its shape asks: spears and the trident are carried couched and thrust
 * forward, the dagger stabs, the axe and the hammer come down over the head, the halberd and the scythe sweep
 * level, and the greatsword cuts a wide diagonal. In first person the hand transform replaces vanilla's swipe
 * (and sets the weapon's resting carry); in third person an arm pose steers the arm through the swing, cancelling
 * the swipe vanilla adds afterward where a different motion is wanted.
 *
 * <p>Every number here can be overridden while the showcase client runs, from {@code <gameDir>/anim-tuning.json}
 * ({@code {"thrust.first.idle.rx": -70}}), so the carries can be tuned against screenshots without a restart.
 */
public final class WeaponAnimations {
    private static final boolean TUNING = Boolean.getBoolean("tribalpower.showcaseVerification");
    private static final Map<String, Float> TUNED = new HashMap<>();
    private static long tunedAt;

    private WeaponAnimations() {}

    public static void register(RegisterClientExtensionsEvent event) {
        for (WeaponKind kind : WeaponKind.values())
            event.registerItem(new Extensions(kind.swing), ModItems.SPIRITGEAR_WEAPONS.get(kind).get());
        event.registerItem(new CrossbowAnimation(), tk.darrow.tribalpower.kit.KitRegistry.PULSE_CROSSBOW.get());
    }

    private record Extensions(WeaponKind.Swing swing) implements IClientItemExtensions {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return WeaponPoses.of(swing);
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                               float partialTick, float equipProcess, float swingProcess) {
            firstPerson(poseStack, arm, equipProcess, swingProcess, swing);
            return true;
        }
    }

    /** A tunable number: the file's value while tuning, else the default written here. */
    private static float v(String key, float fallback) {
        if (!TUNING) return fallback;
        long now = System.currentTimeMillis();
        if (now - tunedAt > 500) {
            tunedAt = now;
            try {
                var file = Minecraft.getInstance().gameDirectory.toPath().resolve("anim-tuning.json");
                TUNED.clear();
                if (java.nio.file.Files.isRegularFile(file)) {
                    var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
                    for (var entry : json.entrySet()) TUNED.put(entry.getKey(), entry.getValue().getAsFloat());
                }
            } catch (Exception ignored) { }
        }
        return TUNED.getOrDefault(key, fallback);
    }

    private static String name(WeaponKind.Swing kind) { return kind.name().toLowerCase(Locale.ROOT); }

    /**
     * The in-hand item: vanilla's equip offset, then the kind's resting carry, then its motion instead of the
     * swipe. {@code s} runs out and back over the swing; {@code w} rises fast and falls slow.
     */
    static void firstPerson(PoseStack pose, HumanoidArm arm, float equip, float swing, WeaponKind.Swing kind) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float s = Mth.sin(swing * Mth.PI);
        float w = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        String k = name(kind);
        pose.translate(i * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        // the resting carry: where the weapon sits in the hand when nothing is happening
        float idleRx = 0, idleRy = 0, idleRz = 0, idleTx = 0, idleTy = 0, idleTz = 0;
        float swRx = 0, swRy = 0, swRz = 0, swTx = 0, swTy = 0, swTz = 0;
        switch (kind) {
            // couched: the shaft laid along the line of sight, tip out ahead, and the thrust runs it forward
            case THRUST -> { idleRx = -90; idleRy = -8; idleRz = 42; idleTx = -0.05F; idleTy = -0.06F; idleTz = -0.35F;
                             swRx = 0; swRy = -10; swTx = -0.2F; swTy = 0.02F; swTz = -1.1F; }
            case STAB -> { idleRx = -85; idleRy = -5; idleRz = 45; idleTy = -0.02F; idleTz = -0.15F;
                           swRy = -12; swTx = -0.15F; swTz = -0.6F; }
            case CHOP -> { swRy = -12; }
            case SWEEP -> { swTy = -0.08F; swTz = -0.5F; swRz = -35; swRx = -15; }
            case SLASH -> { }
        }
        float amount = kind == WeaponKind.Swing.CHOP || kind == WeaponKind.Swing.SLASH ? w : s;
        pose.translate(i * (v(k + ".first.idle.tx", idleTx) + v(k + ".first.swing.tx", swTx) * amount),
                v(k + ".first.idle.ty", idleTy) + v(k + ".first.swing.ty", swTy) * amount,
                v(k + ".first.idle.tz", idleTz) + v(k + ".first.swing.tz", swTz) * amount);
        pose.mulPose(Axis.YP.rotationDegrees(i * (v(k + ".first.idle.ry", idleRy) + v(k + ".first.swing.ry", swRy) * amount)));
        pose.mulPose(Axis.XP.rotationDegrees(v(k + ".first.idle.rx", idleRx) + v(k + ".first.swing.rx", swRx) * amount));
        pose.mulPose(Axis.ZP.rotationDegrees(i * (v(k + ".first.idle.rz", idleRz) + v(k + ".first.swing.rz", swRz) * amount)));
        switch (kind) {
            case CHOP -> {
                // up over the head on the wind-up, then down and forward on the fall
                float raise = Mth.clamp(w - s * 0.6F, 0, 1);
                pose.translate(0, v(k + ".first.raise", 0.45F) * raise - v(k + ".first.drop", 0.55F) * s, -v(k + ".first.lunge", 0.35F) * s);
                pose.mulPose(Axis.XP.rotationDegrees(-v(k + ".first.back", 85F) * raise + v(k + ".first.down", 60F) * s));
            }
            case SWEEP -> {
                float across = Mth.sin(swing * Mth.PI * 0.5F);
                pose.translate(i * (-0.15F - v(k + ".first.reach", 1.1F) * across) * s, 0, 0);
                pose.mulPose(Axis.YP.rotationDegrees(i * (30.0F + v(k + ".first.arc", 95F) * across) * s));
            }
            case SLASH -> {
                pose.translate(i * -0.55F * w, 0.25F * w - 0.3F * s, -0.3F * s);
                pose.mulPose(Axis.YP.rotationDegrees(i * (45.0F + w * -30.0F)));
                pose.mulPose(Axis.ZP.rotationDegrees(i * w * -35.0F));
                pose.mulPose(Axis.XP.rotationDegrees(w * -v(k + ".first.arc", 95F)));
                pose.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
            }
            default -> { }
        }
    }

    /**
     * The player model's arm. Runs before vanilla's attack animation, which will still subtract its swipe
     * ({@code f1 * 1.2 + f2} from the pitch, a roll of {@code sin(t * pi) * -0.4}); a kind that does not swipe
     * adds those back first and sets the pitch and yaw it wants.
     */
    static void thirdPerson(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm, WeaponKind.Swing kind) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        String k = name(kind);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        // the ITEM pose vanilla would have used: the arm lowered a little around whatever it holds
        part.xRot = part.xRot * 0.5F - Mth.PI / 10.0F;
        part.yRot = 0.0F;
        HumanoidArm attackArm = entity.swingingArm == InteractionHand.OFF_HAND ? entity.getMainArm().getOpposite() : entity.getMainArm();
        float t = arm == attackArm ? model.attackTime : 0.0F;
        // at rest a thrusting weapon stands along the hanging arm (its model lies along the forearm); only the
        // thrust itself moves the arm, straight out and back
        if (t <= 0.0F) return;
        float f = 1.0F - t;
        f *= f;
        f *= f;
        f = 1.0F - f;
        float f1 = Mth.sin(f * Mth.PI);
        float f2 = Mth.sin(t * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;
        float swipe = f1 * 1.2F + f2;
        float roll = Mth.sin(t * Mth.PI) * -0.4F;
        float s = Mth.sin(t * Mth.PI);
        float rest = part.xRot;
        switch (kind) {
            case THRUST, STAB -> {
                // the arm straightens forward and comes back; no chop, no roll
                float forward = v(k + ".third.forward", kind == WeaponKind.Swing.STAB ? -1.35F : -1.5F);
                part.xRot = Mth.lerp(s, rest, forward) + swipe;
                part.yRot = i * v(k + ".third.yaw", -0.2F) * s;
                part.zRot = -roll;
            }
            case CHOP -> {
                // over the head and down: vanilla's swipe, much bigger, with the roll kept
                part.xRot = rest - swipe * v(k + ".third.reach", 1.6F);
            }
            case SWEEP -> {
                // level and across: the arm held out forward, yaw carrying it from one side to the other
                part.xRot = Mth.lerp(s, rest, v(k + ".third.level", -1.25F)) + swipe;
                part.yRot = i * (v(k + ".third.from", 0.9F) - v(k + ".third.arc", 1.8F) * f);
                part.zRot = -roll + i * -0.2F * s;
            }
            case SLASH -> {
                part.xRot = rest - swipe * v(k + ".third.reach", 0.5F);
                part.zRot = i * -0.15F * s;
            }
        }
    }
}
