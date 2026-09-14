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
    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final ResourceLocation MOON = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
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
            for (int layer = 0; layer < layers.length; layer++) {
                float weight = Math.clamp(weights[layer], 0F, 1F);
                if (weight < .001F) continue;
                sum += weight;
                float alpha = weight / sum; // Exact weighted crossfade, including healing.
                RenderSystem.setShaderTexture(0, layers[layer]);
                var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                for (int i = 0; i < MESH.length; i += 5)
                    buffer.addVertex(matrix, MESH[i], MESH[i+1], MESH[i+2])
                            .setUv(MESH[i+3], MESH[i+4]).setColor(light, light, light, alpha);
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            }
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            float celestial = level.getTimeOfDay(partial);
            celestial(matrix, SUN, celestial, 8, 0, 0, 1, 1, light);
            int phase = Math.floorMod(level.getMoonPhase(), 8);
            celestial(matrix, MOON, celestial + .5F, 6,
                    (phase % 4) / 4F, (phase / 4) / 2F,
                    (phase % 4 + 1) / 4F, (phase / 4 + 1) / 2F, light);
        } finally {
            RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            if (oldShader != null) RenderSystem.setShader(() -> oldShader);
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend();
        }
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

    private static void celestial(Matrix4f matrix, ResourceLocation texture, float time, float size,
                                  float u0, float v0, float u1, float v1, float alpha) {
        double angle = time * Math.PI * 2;
        float cosine = (float) Math.cos(angle), sine = (float) Math.sin(angle);
        // Local vertical is tangent to the celestial orbit, so the disc never collapses to a sliver.
        RenderSystem.setShaderTexture(0, texture);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int corner = 0; corner < 4; corner++) {
            float x = (corner == 0 || corner == 3 ? -size : size);
            float vertical = corner < 2 ? -size : size;
            buffer.addVertex(matrix, x, cosine * 90 + sine * vertical, sine * 90 - cosine * vertical)
                    .setUv(corner == 0 || corner == 3 ? u0 : u1, corner < 2 ? v1 : v0)
                    .setColor(1F, 1F, 1F, alpha);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}

