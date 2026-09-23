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
 * The ley threads, drawn only for whoever is looking through a lens or ley goggles. Each vein is
 * one thread in its totem's colour. Two soft ribbons cross inside it and roll slowly, so it reads
 * as a round cord from any side without a second colour. A node of that same colour breathes along it.
 */
public final class LeyRopeRenderer {
    /** Soft half-width. The old braid reached about this far from its centre. */
    private static final double HALO = 1.9;
    private static final double CORE = 0.62;
    /** A gentle roll, not a tight braid. One turn about every 35 blocks, plus the slow clock. */
    private static final double TWIST = 0.18;
    /** Radians of roll per tick. About one full turn every nine seconds. */
    private static final double FLOW = 0.034;
    private static final double SHIMMER = 0.054;
    private static final double PULSE = 0.042;
    /**
     * Totem colours, in {@link tk.darrow.tribalpower.api.pulse.Attunement} order:
     * earth, fire, water, air, spirit, loom. Halo, then the hotter core.
     */
    private static final float[][] HALO_RGB = {
            {0.28F, 0.72F, 0.32F},
            {0.95F, 0.38F, 0.08F},
            {0.10F, 0.62F, 0.78F},
            {0.70F, 0.88F, 0.96F},
            {0.58F, 0.32F, 0.92F},
            {0.92F, 0.68F, 0.16F}
    };
    private static final float[][] CORE_RGB = {
            {0.55F, 0.95F, 0.42F},
            {1.00F, 0.72F, 0.28F},
            {0.40F, 0.95F, 1.00F},
            {0.90F, 0.97F, 1.00F},
            {0.82F, 0.58F, 1.00F},
            {1.00F, 0.88F, 0.40F}
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
            var magnets = tk.darrow.tribalpower.ley.LeyMagnets.near(mc.level, mc.player.blockPosition());
            for (LeyRopePayload.Rope raw : ropes) {
                LeyField.Rope rope = new LeyField.Rope(raw.x(), raw.y(), raw.z(), raw.angle(), raw.amp(), raw.freq(),
                        raw.phase(), raw.yAmp(), raw.yFreq(), raw.travel(), raw.voice());
                draw(buffer, matrix, rope, ticks, look, tk.darrow.tribalpower.ley.LeyMagnets.pulls(magnets, rope));
            }
            var mesh = buffer.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
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
                             double ticks, Vec3 look, java.util.List<tk.darrow.tribalpower.ley.LeyMagnets.Pull> pulls) {
        double from = Math.max(0, rope.travel() - 110);
        double to = Math.min(LeyField.REACH, rope.travel() + 110);
        double step = 1.25;
        int voice = rope.voice();
        if (voice < 0 || voice >= HALO_RGB.length) voice = 0;
        float[] halo = HALO_RGB[voice];
        float[] core = CORE_RGB[voice];
        for (double t = from; t < to; t += step) {
            double t2 = Math.min(to, t + step);
            Vec3 a = tk.darrow.tribalpower.ley.LeyMagnets.apply(rope, t, pulls);
            Vec3 b = tk.darrow.tribalpower.ley.LeyMagnets.apply(rope, t2, pulls);
            float wave = 0.62F + 0.38F * (float) Math.pow(Math.max(0, Math.sin(t * 0.22 - ticks * SHIMMER)), 1.35);
            double spin = t * TWIST - ticks * FLOW;
            cord(buffer, matrix, a, b, spin, halo[0], halo[1], halo[2], 0.20F * wave, HALO);
            cord(buffer, matrix, a, b, spin, core[0], core[1], core[2], 0.50F * wave, CORE);
            int mark = (int) Math.floor(t / 18.0);
            if (mark != (int) Math.floor((t - step) / 18.0)) {
                double node = mark * 18.0;
                if (node >= 0 && node <= LeyField.REACH) {
                    float pulse = 0.84F + 0.16F * (float) Math.sin(ticks * PULSE + node * 0.11);
                    glow(buffer, matrix, tk.darrow.tribalpower.ley.LeyMagnets.apply(rope, node, pulls), look, 0.5 * pulse, halo, core);
                }
            }
        }
    }

    /** Two crossed ribbons, bright on the spine and clear at the rim, rolled around the thread. */
    private static void cord(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix,
                             Vec3 a, Vec3 b, double spin, float r, float g, float bl, float alpha, double half) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4) return;
        dx /= len; dy /= len; dz /= len;
        double px, py, pz;
        if (Math.abs(dy) < 0.92) {
            px = -dz; py = 0; pz = dx;
        } else {
            px = 1; py = 0; pz = 0;
        }
        double plen = Math.sqrt(px * px + py * py + pz * pz);
        px /= plen; py /= plen; pz /= plen;
        double bx = dy * pz - dz * py;
        double by = dz * px - dx * pz;
        double bz = dx * py - dy * px;
        double c = Math.cos(spin), s = Math.sin(spin);
        ribbon(buffer, matrix, a, b, px * c + bx * s, py * c + by * s, pz * c + bz * s, half, r, g, bl, alpha);
        ribbon(buffer, matrix, a, b, bx * c - px * s, by * c - py * s, bz * c - pz * s, half, r, g, bl, alpha);
    }

    /** One ribbon across the spine: clear at the rim, full colour on the centre line. */
    private static void ribbon(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix,
                               Vec3 a, Vec3 b, double nx, double ny, double nz,
                               double half, float r, float g, float bl, float alpha) {
        double ox = nx * half, oy = ny * half, oz = nz * half;
        quad(buffer, matrix,
                a.x - ox, a.y - oy, a.z - oz, b.x - ox, b.y - oy, b.z - oz,
                b.x, b.y, b.z, a.x, a.y, a.z,
                r, g, bl, alpha, 0F);
        quad(buffer, matrix,
                a.x, a.y, a.z, b.x, b.y, b.z,
                b.x + ox, b.y + oy, b.z + oz, a.x + ox, a.y + oy, a.z + oz,
                r, g, bl, 0F, alpha);
    }

    /**
     * A soft round light. Three discs, each a ring of wedges bright at the centre and clear at the rim,
     * so the node blooms instead of sitting there as a white square.
     */
    private static void glow(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix, Vec3 at, Vec3 look,
                             double size, float[] halo, float[] core) {
        Vec3 lookN = look.normalize();
        Vec3 right = lookN.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 camUp = right.cross(lookN).normalize();
        disc(buffer, matrix, at, right, camUp, size * 2.4, halo[0], halo[1], halo[2], 0.14F);
        disc(buffer, matrix, at, right, camUp, size * 0.95, core[0], core[1], core[2], 0.42F);
        float hr = Math.min(1F, core[0] * 0.4F + 0.6F);
        float hg = Math.min(1F, core[1] * 0.4F + 0.6F);
        float hb = Math.min(1F, core[2] * 0.4F + 0.6F);
        disc(buffer, matrix, at, right, camUp, size * 0.32, hr, hg, hb, 0.62F);
    }

    private static void disc(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix,
                             Vec3 at, Vec3 right, Vec3 up, double radius, float r, float g, float b, float alpha) {
        int slices = 8;
        for (int i = 0; i < slices; i++) {
            double a0 = i * (Math.PI * 2 / slices);
            double a1 = (i + 1) * (Math.PI * 2 / slices);
            Vec3 e0 = at.add(right.scale(Math.cos(a0) * radius)).add(up.scale(Math.sin(a0) * radius));
            Vec3 e1 = at.add(right.scale(Math.cos(a1) * radius)).add(up.scale(Math.sin(a1) * radius));
            wedge(buffer, matrix, at, e0, e1, r, g, b, alpha);
        }
    }

    /** One slice of a disc: full colour at the centre, nothing at the rim. */
    private static void wedge(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix,
                              Vec3 center, Vec3 e0, Vec3 e1, float r, float g, float b, float alpha) {
        buffer.addVertex(matrix, (float) center.x, (float) center.y, (float) center.z).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, (float) e0.x, (float) e0.y, (float) e0.z).setColor(r, g, b, 0F);
        buffer.addVertex(matrix, (float) e1.x, (float) e1.y, (float) e1.z).setColor(r, g, b, 0F);
        buffer.addVertex(matrix, (float) center.x, (float) center.y, (float) center.z).setColor(r, g, b, alpha);
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
