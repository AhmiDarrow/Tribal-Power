package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import tk.darrow.tribalpower.ley.LeyField;
import tk.darrow.tribalpower.ley.LeyRopePayload;
import tk.darrow.tribalpower.ley.LeyRopes;

/**
 * The ley ropes, drawn only for whoever is looking through a lens or ley goggles. Four strands
 * twist along each vein and the twist scrolls with the clock, so the rope is always flowing.
 * A bright node sits every dozen blocks and pulses in place while the braid runs through it.
 */
public final class LeyRopeRenderer {
    private static final int STRANDS = 4;
    private static final double RADIUS = 1.65;
    private static final double HALF = 0.42;
    /** One full turn of the braid about every 15 blocks. */
    private static final double TWIST = 0.42;
    /** How fast the braid runs along the rope, in turns of phase per tick. */
    private static final double FLOW = 0.28;
    private static final float[][] COLOUR = {
            {0.95F, 0.42F, 0.72F},
            {0.28F, 0.86F, 0.38F},
            {0.98F, 0.78F, 0.28F},
            {0.28F, 0.82F, 0.98F}
    };

    private LeyRopeRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !LeyRopes.sees(mc.player)) return;
        var ropes = LeyRopePayload.latest.ropes();
        if (ropes.isEmpty()) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double ticks = mc.level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        var poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poses.last().pose();

        var oldShader = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            var rawLook = event.getCamera().getLookVector();
        Vec3 look = new Vec3(rawLook.x(), rawLook.y(), rawLook.z());
            for (LeyRopePayload.Rope raw : ropes) {
                LeyField.Rope rope = new LeyField.Rope(raw.x(), raw.y(), raw.z(), raw.angle(), raw.amp(), raw.freq(),
                        raw.phase(), raw.yAmp(), raw.yFreq(), raw.travel());
                draw(buffer, matrix, rope, ticks, look);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            poses.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            if (oldShader != null) RenderSystem.setShader(() -> oldShader);
        }
    }

    private static void draw(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix, LeyField.Rope rope,
                             double ticks, Vec3 look) {
        double from = Math.max(0, rope.travel() - 36);
        double to = Math.min(LeyField.REACH, rope.travel() + 36);
        double step = 0.85;
        double sx = -Math.sin(rope.angle());
        double sz = Math.cos(rope.angle());
        for (double t = from; t < to; t += step) {
            double t2 = Math.min(to, t + step);
            Vec3 a = LeyField.along(rope, t);
            Vec3 b = LeyField.along(rope, t2);
            float wave = 0.42F + 0.58F * (float) Math.pow(Math.max(0, Math.sin(t * 0.55 - ticks * 0.45)), 1.35);
            for (int s = 0; s < STRANDS; s++) {
                float[] colour = COLOUR[s];
                Vec3 pa = strand(a, sx, sz, t, s, ticks);
                Vec3 pb = strand(b, sx, sz, t2, s, ticks);
                ribbon(buffer, matrix, pa, pb, colour[0], colour[1], colour[2], wave);
            }
            int mark = (int) Math.floor(t / 12.0);
            if (mark != (int) Math.floor((t - step) / 12.0)) {
                double node = mark * 12.0;
                if (node >= 0 && node <= LeyField.REACH) {
                    float pulse = 0.72F + 0.28F * (float) Math.sin(ticks * 0.35 + node);
                    orb(buffer, matrix, LeyField.along(rope, node), look, 0.7 * pulse);
                }
            }
        }
    }

    private static Vec3 strand(Vec3 axis, double sx, double sz, double t, int strand, double ticks) {
        double ang = t * TWIST - ticks * FLOW + strand * (Math.PI * 2 / STRANDS);
        return new Vec3(axis.x + sx * Math.cos(ang) * RADIUS, axis.y + Math.sin(ang) * RADIUS,
                axis.z + sz * Math.cos(ang) * RADIUS);
    }

    /** A short ribbon lying across the strand so the braid has thickness from the side. */
    private static void ribbon(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix,
                               Vec3 a, Vec3 b, float r, float g, float bl, float alpha) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4) return;
        dx /= len; dy /= len; dz /= len;
        double nx = -dz, nz = dx;
        double nlen = Math.sqrt(nx * nx + nz * nz);
        if (nlen < 1.0e-4) { nx = 1; nz = 0; nlen = 1; }
        nx = nx / nlen * HALF; nz = nz / nlen * HALF;
        float a0 = alpha, a1 = alpha * 0.15F;
        quad(buffer, matrix,
                a.x + nx, a.y, a.z + nz, b.x + nx, b.y, b.z + nz,
                b.x - nx, b.y, b.z - nz, a.x - nx, a.y, a.z - nz,
                r, g, bl, a0, a1);
    }

    private static void orb(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix, Vec3 at, Vec3 look, double size) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0e-4) right = new Vec3(1, 0, 0);
        right = right.normalize().scale(size);
        Vec3 camUp = right.cross(look).normalize().scale(size);
        quad(buffer, matrix,
                at.x - right.x - camUp.x, at.y - right.y - camUp.y, at.z - right.z - camUp.z,
                at.x + right.x - camUp.x, at.y + right.y - camUp.y, at.z + right.z - camUp.z,
                at.x + right.x + camUp.x, at.y + right.y + camUp.y, at.z + right.z + camUp.z,
                at.x - right.x + camUp.x, at.y - right.y + camUp.y, at.z - right.z + camUp.z,
                1F, 0.97F, 0.88F, 0.95F, 0.2F);
    }

    private static void quad(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3,
                             float r, float g, float b, float aMid, float aEdge) {
        buffer.addVertex(matrix, (float) x0, (float) y0, (float) z0).setColor(r, g, b, aEdge);
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(r, g, b, aEdge);
        buffer.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(r, g, b, aMid);
        buffer.addVertex(matrix, (float) x3, (float) y3, (float) z3).setColor(r, g, b, aMid);
    }
}
