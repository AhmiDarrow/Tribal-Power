package tk.darrow.tribalpower.client.codex;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.pattern.BlockPredicate;
import tk.darrow.tribalpower.pattern.ModPatterns;
import tk.darrow.tribalpower.pattern.RitualPattern;

/**
 * Stepped 3D builds for the Codex: every step adds blocks (they drop into
 * place), outlines what it is talking about, can float the item you use, and says in one sentence what is
 * happening. The reader moves through steps; the view turns slowly and can be dragged.
 */
public final class CodexScene {
    private static final Map<String, BlockState> STATES = new LinkedHashMap<>();

    private CodexScene() {}

    /** The blocks standing at a step: every placement up to it, minus removals. */
    public static Map<BlockPos, BlockState> blocksAt(List<CodexBook.Step> steps, int step) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int i = 0; i <= step && i < steps.size(); i++) {
            for (BlockPos pos : steps.get(i).remove()) blocks.remove(pos);
            for (CodexBook.Placed placed : steps.get(i).place()) blocks.put(placed.pos(), state(placed.state()));
        }
        return blocks;
    }

    public static BlockState state(String text) {
        return STATES.computeIfAbsent(text, key -> {
            try {
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), key.contains(":") ? key : "tribalpower:" + key, false).blockState();
            } catch (Exception error) {
                TribalPower.LOGGER.warn("Spirit Codex scene names an unknown block {}", key);
                return Blocks.BARRIER.defaultBlockState();
            }
        });
    }

    /**
     * Draws a step into the rectangle. {@code fresh} is how long the step has been showing (seconds), used for
     * the drop-in; {@code yaw} is the view angle.
     */
    public static void draw(GuiGraphics g, List<CodexBook.Step> steps, int step, int x, int y, int w, int h,
                            float yaw, double fresh, double time) {
        if (steps.isEmpty()) return;
        Map<BlockPos, BlockState> all = new LinkedHashMap<>();
        for (int i = 0; i < steps.size(); i++) all.putAll(blocksAt(steps, i));
        if (all.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : all.keySet()) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        float sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
        // Fit the turning footprint (its diagonal) and the height, whichever is tighter.
        float diagonal = (float) Math.sqrt(sx * sx + sz * sz);
        float scale = Math.min(w / Math.max(1.2F, diagonal * 1.05F), h / Math.max(1.2F, diagonal * 0.5F + sy * 0.95F)) * 0.92F;

        CodexBook.Step current = steps.get(Math.min(step, steps.size() - 1));
        List<BlockPos> added = current.place().stream().map(CodexBook.Placed::pos).toList();
        Map<BlockPos, BlockState> shown = blocksAt(steps, step);
        Minecraft mc = Minecraft.getInstance();
        var buffers = mc.renderBuffers().bufferSource();
        var pose = g.pose();

        g.enableScissor(x, y, x + w, y + h);
        pose.pushPose();
        pose.translate(x + w / 2F, y + h / 2F + sy * scale * 0.1F, 400);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(28));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-(minX + sx / 2F), -(minY + sy / 2F), -(minZ + sz / 2F));
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        for (Map.Entry<BlockPos, BlockState> block : shown.entrySet()) {
            BlockPos p = block.getKey();
            double drop = added.contains(p) ? Math.max(0, 1 - fresh / 0.35) : 0;
            pose.pushPose();
            pose.translate(p.getX(), p.getY() + drop * drop * 1.5, p.getZ());
            mc.getBlockRenderer().renderSingleBlock(block.getValue(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();
        // Fluids have no block model, so draw them here, after the solid blocks they sit among.
        for (Map.Entry<BlockPos, BlockState> block : shown.entrySet()) {
            if (block.getValue().getFluidState().isEmpty()) continue;
            BlockPos p = block.getKey();
            double drop = added.contains(p) ? Math.max(0, 1 - fresh / 0.35) : 0;
            pose.pushPose();
            pose.translate(p.getX(), p.getY() + drop * drop * 1.5, p.getZ());
            fluid(pose, buffers, block.getValue().getFluidState(), shown, p);
            pose.popPose();
        }
        buffers.endBatch();
        float pulse = 0.55F + 0.45F * (float) Math.sin(time * 5);
        for (BlockPos p : current.highlight()) {
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(p).inflate(0.02),
                    1.0F, 0.86F, 0.55F, pulse);
        }
        buffers.endBatch();
        if (current.use() != null && !current.use().item().isEmpty()) {
            var item = BuiltInRegistries.ITEM.get(CodexBook.itemId(current.use().item()));
            BlockPos p = current.use().pos();
            pose.pushPose();
            pose.translate(p.getX() + 0.5, p.getY() + 1.45 + 0.08 * Math.sin(time * 3), p.getZ() + 0.5);
            pose.mulPose(Axis.YP.rotationDegrees((float) (time * 60 % 360)));
            pose.scale(0.9F, 0.9F, 0.9F);
            mc.getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
            pose.popPose();
            buffers.endBatch();
        }
        pose.popPose();
        // The scene sits in front of the page; clear its depth so later text and tooltips draw over it.
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        g.disableScissor();
    }

    /**
     * A fluid block as a tinted cube of its still texture: the surface sits a little low unless more of the
     * same fluid is above, and faces against the same fluid are left out so a pool reads as one body.
     */
    private static void fluid(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                              net.minecraft.world.level.material.FluidState fluid, Map<BlockPos, BlockState> shown, BlockPos at) {
        var looks = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        var sprite = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(looks.getStillTexture());
        int tint = looks.getTintColor();
        float r = (tint >> 16 & 255) / 255F, gr = (tint >> 8 & 255) / 255F, b = (tint & 255) / 255F;
        float a = (tint >>> 24) == 0 ? 0.8F : Math.max(0.55F, (tint >>> 24) / 255F);
        var consumer = buffers.getBuffer(RenderType.translucent());
        var last = pose.last();
        java.util.function.Predicate<net.minecraft.core.Direction> same = d -> {
            BlockState next = shown.get(at.relative(d));
            return next != null && next.getFluidState().getType().isSame(fluid.getType());
        };
        float top = same.test(net.minecraft.core.Direction.UP) ? 1F : 0.875F;
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float[][][] faces = {
                {{0, top, 0}, {0, top, 1}, {1, top, 1}, {1, top, 0}, {0, 1, 0}},       // up
                {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, -1, 0}},            // down
                {{0, top, 0}, {1, top, 0}, {1, 0, 0}, {0, 0, 0}, {0, 0, -1}},       // north
                {{1, top, 1}, {0, top, 1}, {0, 0, 1}, {1, 0, 1}, {0, 0, 1}},        // south
                {{0, top, 1}, {0, top, 0}, {0, 0, 0}, {0, 0, 1}, {-1, 0, 0}},       // west
                {{1, top, 0}, {1, top, 1}, {1, 0, 1}, {1, 0, 0}, {1, 0, 0}},        // east
        };
        net.minecraft.core.Direction[] sides = {net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN,
                net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST,
                net.minecraft.core.Direction.EAST};
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (int f = 0; f < faces.length; f++) {
            if (same.test(sides[f])) continue;
            float[] n = faces[f][4];
            // Both windings: the pool is seen from any angle the reader turns it to.
            for (int pass = 0; pass < 2; pass++)
                for (int k = 0; k < 4; k++) {
                    int i = pass == 0 ? k : 3 - k;
                    float[] v = faces[f][i];
                    consumer.addVertex(last, v[0], v[1], v[2]).setColor(r, gr, b, a).setUv(uv[i][0], uv[i][1])
                            .setLight(LightTexture.FULL_BRIGHT).setNormal(last, n[0], n[1], n[2]);
                }
        }
    }

    /**
     * A ritual pattern tier as steps: its anchor first, then each kind of block in turn, so the reader
     * builds it the way they would in the world.
     */
    public static List<CodexBook.Step> pattern(String id, int tierNumber) {
        RitualPattern pattern = switch (id) {
            case "stone_font" -> ModPatterns.STONE_FONT;
            case "listening_pit" -> ModPatterns.LISTENING_PIT;
            case "rite_circle" -> ModPatterns.RITE_CIRCLE;
            case "voice_ring" -> ModPatterns.VOICE_RING;
            case "shatter_array" -> ModPatterns.SHATTER_ARRAY;
            case "way_gate" -> ModPatterns.WAY_GATE;
            case "far_gate" -> ModPatterns.FAR_GATE;
            default -> null;
        };
        if (pattern == null) return List.of();
        RitualPattern.Tier tier = pattern.tier(tierNumber);
        if (tier == null) tier = pattern.tiers().getLast();
        List<CodexBook.Step> steps = new ArrayList<>();
        BlockState anchor = stateOf(tier.anchor().predicate());
        steps.add(new CodexBook.Step(net.minecraft.network.chat.Component.translatable("gui.tribalpower.codex.scene_start", tier.anchor().predicate().description()).getString(),
                List.of(new CodexBook.Placed(BlockPos.ZERO, key(anchor))), List.of(), List.of(BlockPos.ZERO), null));
        Map<String, List<RitualPattern.Cell>> groups = new LinkedHashMap<>();
        for (RitualPattern.Cell cell : tier.cells()) {
            if (cell.predicate().trivial() || cell.offset().equals(Vec3i.ZERO)) continue;
            groups.computeIfAbsent(cell.predicate().description().getString(), k -> new ArrayList<>()).add(cell);
        }
        groups.forEach((description, cells) -> {
            List<CodexBook.Placed> place = new ArrayList<>();
            List<BlockPos> highlight = new ArrayList<>();
            for (RitualPattern.Cell cell : cells) {
                BlockState state = stateOf(cell.predicate());
                if (state.isAir()) continue;
                BlockPos pos = new BlockPos(cell.offset());
                place.add(new CodexBook.Placed(pos, key(state)));
                highlight.add(pos);
            }
            if (place.isEmpty()) return;
            String caption = net.minecraft.network.chat.Component.translatable("gui.tribalpower.codex.scene_add", description, cells.size()).getString();
            steps.add(new CodexBook.Step(caption, place, List.of(), highlight, null));
        });
        return steps;
    }

    private static BlockState stateOf(BlockPredicate predicate) {
        ItemStack icon = predicate.icon();
        return icon.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    private static String key(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        STATES.putIfAbsent(id, state);
        return id;
    }
}
