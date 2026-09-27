package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Camera-centred HD panoramas; geometry is calculated once, not once per frame. */
public final class PanoramicSky {
    private static net.minecraft.client.renderer.ShaderInstance panoramaShader;

    public static void registerShaders(net.neoforged.neoforge.client.event.RegisterShadersEvent event) {
        try { event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("tribalpower", "panoramic_sky"),
                DefaultVertexFormat.POSITION_TEX_COLOR), shader -> panoramaShader = shader); } catch (java.io.IOException error) { throw new java.io.UncheckedIOException("Unable to load panoramic sky shader", error); }
    }
    private static final int SEGMENTS = 96, RINGS = 48;
    private static final float[] MESH = mesh();
    /** The panorama sphere on the GPU, built once: each frame only binds it and draws (no 18k-vertex re-upload). */
    private static VertexBuffer sphere;
    private static final ResourceLocation SUN = ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/sky/march_sun.png");

    /**
     * One of the March's three moons: its phase atlas (vanilla's 4 x 2 layout, frame 0 full), half-width in sky units,
     * how far its track is tilted from the sun's, and its cycle. A cycle of 0 is the Pale, which keeps vanilla's moon
     * (opposite the sun, the world's own phase) so full-moon rules still read it; the others drift against the sun
     * over {@code cycleDays} and wear the phase their angle to the sun gives them.
     */
    private record Moon(ResourceLocation texture, float size, float tiltDegrees, float cycleDays, float offset) {}

    private static final Moon[] MOONS = {
        new Moon(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/sky/march_moon_ember.png"), 4.8F, -22F, 4F, -0.31F),
        new Moon(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/sky/march_moon_loom.png"), 7.5F, 14F, 9F, 0.19F),
        new Moon(ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/sky/march_moon_pale.png"), 12.0F, 0F, 0F, 0F),
    };
    private PanoramicSky() {}

    static void draw(ClientLevel level, float partial, Matrix4f modelView,
                     ResourceLocation[] layers, float[] weights) {
        Matrix4f matrix = new Matrix4f(modelView).m30(0).m31(0).m32(0);
        var oldShader = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        int oldTexture = RenderSystem.getShaderTexture(0);
        float light = 1 - level.getRainLevel(partial) * .38F;
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
        RenderSystem.setShader(() -> panoramaShader != null ? panoramaShader : GameRenderer.getPositionTexColorShader());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            float sum = 0;
            var shader = panoramaShader != null ? panoramaShader : GameRenderer.getPositionTexColorShader();
            VertexBuffer mesh = sphere();
            mesh.bind();
            for (int layer = 0; layer < layers.length; layer++) {
                float weight = Math.clamp(weights[layer], 0F, 1F);
                if (weight < .001F) continue;
                sum += weight;
                float alpha = weight / sum; // Exact weighted crossfade, including healing.
                RenderSystem.setShaderTexture(0, layers[layer]);
                RenderSystem.setShaderColor(light, light, light, alpha);
                mesh.drawWithShader(matrix, RenderSystem.getProjectionMatrix(), shader);
            }
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            float celestial = level.getTimeOfDay(partial);
            celestial(matrix, SUN, celestial, 20, 0, 0, 0, 1, 1, light);
            // Moons: ordinary alpha, so a dark limb hides the stars; by day they wash out like a daytime moon.
            RenderSystem.defaultBlendFunc();
            float day = tk.darrow.tribalpower.world.MarchSkyMath.dayness(level.getDayTime(), partial);
            float moonAlpha = light * (1 - 0.8F * day);
            double days = (level.getDayTime() + (double) partial) / 24000.0;
            for (Moon moon : MOONS) {
                float elongation;
                int phase;
                if (moon.cycleDays() <= 0) {
                    elongation = .5F;
                    phase = Math.floorMod(level.getMoonPhase(), 8);
                } else {
                    elongation = (float) (((.5 + moon.offset() - days / moon.cycleDays()) % 1 + 1) % 1);
                    phase = Math.floorMod(Math.round((.5F - elongation) * 8), 8);
                }
                celestial(matrix, moon.texture(), celestial + elongation, moon.size(), moon.tiltDegrees(),
                        (phase % 4) / 4F, (phase / 4) / 2F, (phase % 4 + 1) / 4F, (phase / 4 + 1) / 2F, moonAlpha);
            }
        } finally {
            RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            if (oldShader != null) RenderSystem.setShader(() -> oldShader);
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend();
        }
    }

    private static VertexBuffer sphere() {
        if (sphere == null) {
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < MESH.length; i += 5)
                buffer.addVertex(MESH[i], MESH[i+1], MESH[i+2]).setUv(MESH[i+3], MESH[i+4]).setColor(1F, 1F, 1F, 1F);
            sphere = new VertexBuffer(VertexBuffer.Usage.STATIC);
            sphere.bind();
            sphere.upload(buffer.buildOrThrow());
            VertexBuffer.unbind();
        }
        return sphere;
    }

    private static float[] mesh() {
        float[] result = new float[SEGMENTS * RINGS * 4 * 5];
        int n = 0;
        for (int ring = 0; ring < RINGS; ring++) for (int segment = 0; segment < SEGMENTS; segment++) {
            for (int corner = 0; corner < 4; corner++) {
                float u = (segment + (corner == 1 || corner == 2 ? 1 : 0)) / (float) SEGMENTS;
                float v = (ring + (corner >= 2 ? 1 : 0)) / (float) RINGS;
                double longitude = u * Math.PI * 2, latitude = v * Math.PI;
                result[n++] = (float) (Math.sin(latitude) * Math.cos(longitude) * 100);
                result[n++] = (float) (Math.cos(latitude) * 100);
                result[n++] = (float) (Math.sin(latitude) * Math.sin(longitude) * 100);
                result[n++] = u; result[n++] = v;
            }
        }
        return result;
    }

    private static void celestial(Matrix4f matrix, ResourceLocation texture, float time, float size, float tiltDegrees,
                                  float u0, float v0, float u1, float v1, float alpha) {
        double angle = time * Math.PI * 2;
        float cosine = (float) Math.cos(angle), sine = (float) Math.sin(angle);
        if (alpha < .004F || cosine * 90 * Math.cos(Math.toRadians(tiltDegrees)) < -size * 2) return;   // faded out, or under the world
        // a tilted track: the orbit plane turned about the horizon axis it rises on, so moons cross the sky apart
        float tc = (float) Math.cos(Math.toRadians(tiltDegrees)), ts = (float) Math.sin(Math.toRadians(tiltDegrees));
        // Local vertical is tangent to the celestial orbit, so the disc never collapses to a sliver.
        RenderSystem.setShaderTexture(0, texture);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int corner = 0; corner < 4; corner++) {
            float x = (corner == 0 || corner == 3 ? -size : size);
            float vertical = corner < 2 ? -size : size;
            float y = cosine * 90 + sine * vertical;
            buffer.addVertex(matrix, x * tc - y * ts, x * ts + y * tc, sine * 90 - cosine * vertical)
                    .setUv(corner == 0 || corner == 3 ? u0 : u1, corner < 2 ? v1 : v0)
                    .setColor(1F, 1F, 1F, alpha);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}

