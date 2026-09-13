package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import tk.darrow.tribalpower.world.MarchSkyMath;

/** Living lattice sky for The March — teal day, indigo night. Aurora still draws after this. */
public final class MarchSkyRenderer {
    private static final float RADIUS = 100F;
    private static final int RINGS = 14;
    private static final int SEGS = 32;

    private MarchSkyRenderer() {}

    public static boolean draw(ClientLevel level, float partial, Matrix4f modelView, Camera camera, boolean foggy, Runnable setupFog) {
        setupFog.run();
        if (foggy || camera.getFluidInCamera() != FogType.NONE) {
            return true;
        }
        float day = MarchSkyMath.dayness(level.getDayTime(), partial);
        float rain = level.getRainLevel(partial);
        float[] zenith = MarchSkyMath.zenith(day, rain);
        float[] horizon = MarchSkyMath.horizon(day, rain);
        float[] nadir = MarchSkyMath.nadir(day, rain);
        Matrix4f m = new Matrix4f(modelView);
        m.m30(0).m31(0).m32(0);
        var old = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            sphere(buffer, m, zenith, horizon, nadir);
            lattice(buffer, m, MarchSkyMath.latticeAlpha(day));
            float cel = level.getTimeOfDay(partial);
            float sunA = MarchSkyMath.sunAlpha(day, rain);
            if (sunA > 0.02F) {
                disc(buffer, m, cel, 12F, 0.72F, 0.92F, 0.88F, sunA, false);
            }
            float moonA = MarchSkyMath.moonAlpha(day, rain);
            if (moonA > 0.02F) {
                ring(buffer, m, cel + 0.5F, 9F, 11.5F, 0.78F, 0.86F, 1F, moonA);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            if (old != null) {
                RenderSystem.setShader(() -> old);
            }
        }
        return true;
    }

    private static void sphere(com.mojang.blaze3d.vertex.BufferBuilder b, Matrix4f m, float[] zenith, float[] horizon, float[] nadir) {
        for (int ring = 0; ring < RINGS; ring++) {
            float t0 = ring / (float) RINGS;
            float t1 = (ring + 1) / (float) RINGS;
            float a0 = (float) (t0 * Math.PI);
            float a1 = (float) (t1 * Math.PI);
            float y0 = (float) Math.cos(a0) * RADIUS;
            float y1 = (float) Math.cos(a1) * RADIUS;
            float r0 = (float) Math.sin(a0) * RADIUS;
            float r1 = (float) Math.sin(a1) * RADIUS;
            float[] c0 = mix(t0, zenith, horizon, nadir);
            float[] c1 = mix(t1, zenith, horizon, nadir);
            for (int s = 0; s < SEGS; s++) {
                float u0 = (float) (s * Math.PI * 2 / SEGS);
                float u1 = (float) ((s + 1) * Math.PI * 2 / SEGS);
                quad(b, m,
                        r0 * (float) Math.cos(u0), y0, r0 * (float) Math.sin(u0), c0,
                        r0 * (float) Math.cos(u1), y0, r0 * (float) Math.sin(u1), c0,
                        r1 * (float) Math.cos(u1), y1, r1 * (float) Math.sin(u1), c1,
                        r1 * (float) Math.cos(u0), y1, r1 * (float) Math.sin(u0), c1);
            }
        }
    }

    private static float[] mix(float t, float[] zenith, float[] horizon, float[] nadir) {
        if (t <= 0.5F) {
            return lerp3(t * 2F, zenith, horizon);
        }
        return lerp3((t - 0.5F) * 2F, horizon, nadir);
    }

    private static float[] lerp3(float t, float[] a, float[] b) {
        t = Math.clamp(t, 0F, 1F);
        return new float[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t };
    }

    private static void lattice(com.mojang.blaze3d.vertex.BufferBuilder b, Matrix4f m, float alpha) {
        float[] c = { 0.45F, 0.82F, 0.78F, alpha };
        for (int mer = 0; mer < 8; mer++) {
            float u = (float) (mer * Math.PI / 8);
            for (int ring = 0; ring < RINGS; ring++) {
                float t0 = ring / (float) RINGS;
                float t1 = (ring + 1) / (float) RINGS;
                float a0 = (float) (t0 * Math.PI);
                float a1 = (float) (t1 * Math.PI);
                band(b, m, a0, a1, u, 0.012F, c);
            }
        }
        for (int par = 1; par < 6; par++) {
            float a = (float) (par * Math.PI / 7);
            float y = (float) Math.cos(a) * RADIUS;
            float r = (float) Math.sin(a) * RADIUS;
            float w = 0.9F;
            for (int s = 0; s < SEGS; s++) {
                float u0 = (float) (s * Math.PI * 2 / SEGS);
                float u1 = (float) ((s + 1) * Math.PI * 2 / SEGS);
                quad(b, m,
                        (r - w) * (float) Math.cos(u0), y, (r - w) * (float) Math.sin(u0), c,
                        (r + w) * (float) Math.cos(u0), y, (r + w) * (float) Math.sin(u0), c,
                        (r + w) * (float) Math.cos(u1), y, (r + w) * (float) Math.sin(u1), c,
                        (r - w) * (float) Math.cos(u1), y, (r - w) * (float) Math.sin(u1), c);
            }
        }
    }

    private static void band(com.mojang.blaze3d.vertex.BufferBuilder b, Matrix4f m, float a0, float a1, float u, float half, float[] c) {
        float y0 = (float) Math.cos(a0) * RADIUS;
        float y1 = (float) Math.cos(a1) * RADIUS;
        float r0 = (float) Math.sin(a0) * RADIUS;
        float r1 = (float) Math.sin(a1) * RADIUS;
        float u0 = u - half;
        float u1 = u + half;
        quad(b, m,
                r0 * (float) Math.cos(u0), y0, r0 * (float) Math.sin(u0), c,
                r0 * (float) Math.cos(u1), y0, r0 * (float) Math.sin(u1), c,
                r1 * (float) Math.cos(u1), y1, r1 * (float) Math.sin(u1), c,
                r1 * (float) Math.cos(u0), y1, r1 * (float) Math.sin(u0), c);
    }

    private static void disc(com.mojang.blaze3d.vertex.BufferBuilder b, Matrix4f m, float celestial, float size, float r, float g, float bl, float a, boolean below) {
        float ang = celestial * (float) Math.PI * 2;
        float cy = (float) Math.cos(ang) * 80F * (below ? -1 : 1);
        float cz = (float) Math.sin(ang) * 80F * (below ? -1 : 1);
        float[] c = { r, g, bl, a };
        float hx = size, hy = size * 0.15F;
        quad(b, m, -hx, cy - hy, cz, c, hx, cy - hy, cz, c, hx, cy + hy, cz, c, -hx, cy + hy, cz, c);
        float[] halo = { r, g, bl, a * 0.28F };
        float hx2 = size * 2.2F, hy2 = size * 0.45F;
        quad(b, m, -hx2, cy - hy2, cz, halo, hx2, cy - hy2, cz, halo, hx2, cy + hy2, cz, halo, -hx2, cy + hy2, cz, halo);
    }

    private static void ring(com.mojang.blaze3d.vertex.BufferBuilder b, Matrix4f m, float celestial, float inner, float outer, float r, float g, float bl, float a) {
        float ang = celestial * (float) Math.PI * 2;
        float cy = (float) Math.cos(ang) * 80F;
        float cz = (float) Math.sin(ang) * 80F;
        float[] c = { r, g, bl, a };
        int n = 16;
        for (int i = 0; i < n; i++) {
            float u0 = (float) (i * Math.PI * 2 / n);
            float u1 = (float) ((i + 1) * Math.PI * 2 / n);
            quad(b, m,
                    (float) Math.cos(u0) * inner, cy + (float) Math.sin(u0) * inner * 0.2F, cz, c,
                    (float) Math.cos(u0) * outer, cy + (float) Math.sin(u0) * outer * 0.2F, cz, c,
                    (float) Math.cos(u1) * outer, cy + (float) Math.sin(u1) * outer * 0.2F, cz, c,
                    (float) Math.cos(u1) * inner, cy + (float) Math.sin(u1) * inner * 0.2F, cz, c);
        }
    }

    private static void quad(com.mojang.blaze3d.vertex.BufferBuilder b, Matrix4f m,
            float x0, float y0, float z0, float[] c0,
            float x1, float y1, float z1, float[] c1,
            float x2, float y2, float z2, float[] c2,
            float x3, float y3, float z3, float[] c3) {
        vert(b, m, x0, y0, z0, c0);
        vert(b, m, x1, y1, z1, c1);
        vert(b, m, x2, y2, z2, c2);
        vert(b, m, x3, y3, z3, c3);
    }

    private static void vert(com.mojang.blaze3d.vertex.BufferBuilder b, Matrix4f m, float x, float y, float z, float[] c) {
        float a = c.length > 3 ? c[3] : 1F;
        b.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], a);
    }
}
