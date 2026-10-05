package tk.darrow.tribalpower.client.codex;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * The bestiary's creatures, drawn from their own live models rather than a picture or the reagent they drop.
 *
 * <p>Cheap by construction: each kind is one client-side entity, made the first time its page opens, never added
 * to the level and never ticked (so no AI, sounds or particles), and measured once by drawing it into a sink that
 * only keeps its extent. A page draws its creature only while the spread it is on is open, and the codex lets go
 * of all of them when it closes.
 */
public final class CodexCreature {
    private CodexCreature() {}

    /** Seen from a little above, like a scene. */
    private static final float TILT = 18F;

    /** A creature as the codex keeps it: the entity and the box its model fills, in blocks, facing south. */
    private record Shown(LivingEntity entity, float cx, float cy, float cz, float height, float radius) {}

    private static final Map<String, Optional<Shown>> SHOWN = new HashMap<>();
    private static ClientLevel madeIn;

    /** Lets go of every creature the codex made; called when it closes. */
    public static void clear() {
        SHOWN.clear();
        madeIn = null;
    }

    /** The creature's own name, or empty when the id is no living thing. */
    public static Component name(String id) {
        return shown(id).map(s -> s.entity().getType().getDescription()).orElse(Component.empty());
    }

    private static Optional<Shown> shown(String id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();
        if (level != madeIn) {
            SHOWN.clear();
            madeIn = level;
        }
        return SHOWN.computeIfAbsent(id, k -> make(level, k));
    }

    private static Optional<Shown> make(ClientLevel level, String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) return Optional.empty();
        if (!(BuiltInRegistries.ENTITY_TYPE.get(key).create(level) instanceof LivingEntity entity)) return Optional.empty();
        entity.setYRot(0);
        entity.setXRot(0);
        entity.yRotO = entity.xRotO = 0;
        entity.yBodyRot = entity.yBodyRotO = entity.yHeadRot = entity.yHeadRotO = 0;
        Extent e = new Extent();
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            MultiBufferSource extent = type -> e;
            dispatcher.render(entity, 0, 0, 0, 0, 1F, new PoseStack(), extent, LightTexture.FULL_BRIGHT);
        } catch (RuntimeException broken) {
            // a renderer that cannot draw outside the world still gets its hitbox
            e.clear();
        } finally {
            dispatcher.setRenderShadow(true);
        }
        if (e.empty()) {
            float w = entity.getBbWidth() / 2, h = entity.getBbHeight();
            e.add(-w, 0, -w);
            e.add(w, h, w);
        }
        float dx = (e.maxX - e.minX) / 2, dz = (e.maxZ - e.minZ) / 2;
        return Optional.of(new Shown(entity, (e.minX + e.maxX) / 2, (e.minY + e.maxY) / 2, (e.minZ + e.maxZ) / 2,
                Math.max(0.1F, e.maxY - e.minY), Math.max(0.1F, (float) Math.sqrt(dx * dx + dz * dz))));
    }

    /**
     * Draws the creature turning slowly inside {@code w x h} at {@code x, y}, sized so it stays in the box at every
     * angle. {@code yaw} is in degrees; {@code time} in seconds drives its idle movement. Returns whether it drew.
     */
    public static boolean draw(GuiGraphics g, String id, int x, int y, int w, int h, float yaw, double time) {
        Optional<Shown> found = shown(id);
        if (found.isEmpty()) return false;
        Shown s = found.get();
        LivingEntity entity = s.entity();
        double ticks = time * 20;
        entity.tickCount = (int) ticks;
        float partial = (float) (ticks - Math.floor(ticks));

        // the turning footprint and the height, tilted, whichever is tighter; a tiny creature is not blown up
        // past the size a hand-high one would have
        double tilt = Math.toRadians(TILT);
        float tall = (float) (s.height() * Math.cos(tilt) + 2 * s.radius() * Math.sin(tilt));
        float scale = Math.min(w * 0.88F / (2 * s.radius()), h * 0.86F / Math.max(0.6F, tall));

        var pose = g.pose();
        g.enableScissor(x, y, x + w, y + h);
        pose.pushPose();
        pose.translate(x + w / 2F, y + h / 2F, 400);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(TILT));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-s.cx(), -s.cy(), -s.cz());
        Lighting.setupForEntityInInventory();
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        RenderSystem.enableDepthTest();
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0, partial, pose, g.bufferSource(), LightTexture.FULL_BRIGHT));
        g.flush();
        dispatcher.setRenderShadow(true);
        pose.popPose();
        Lighting.setupFor3DItems();
        g.disableScissor();
        return true;
    }

    /** A sink that keeps only how far the model reaches: what the creature needs of the box, not its vertices. */
    private static final class Extent implements VertexConsumer {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        boolean empty() {
            return minX > maxX;
        }

        void clear() {
            minX = minY = minZ = Float.MAX_VALUE;
            maxX = maxY = maxZ = -Float.MAX_VALUE;
        }

        void add(float x, float y, float z) {
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { add(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
