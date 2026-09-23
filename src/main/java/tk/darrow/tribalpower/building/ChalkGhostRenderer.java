package tk.darrow.tribalpower.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the Builder's Chalk layout as a hologram: a ghost block in every space the shape still needs,
 * so you can lay real blocks inside them and watch the outline empty out.
 *
 * <p>Entirely client-side. The shape, size and mark live on the held stack's data component, which the
 * server already syncs to whoever is holding it, so the hologram costs the server nothing.
 *
 * <p><b>The standing plan is worked out rarely and drawn often.</b> Deciding which spaces are still
 * open means a block lookup for every offset in the shape, and a size-128 dome is a hundred thousand of
 * them — far too much to redo sixty times a second. So the plan is rebuilt only when the shape, the
 * size or the origin changes, or every {@link #REFRESH} ticks to notice blocks laid since; every frame
 * in between just redraws the packed positions it already holds, allocating nothing.
 *
 * <p>Register {@code ChalkGhostRenderer::render} on the NeoForge bus from the client mod class only.
 */
public final class ChalkGhostRenderer {
    /** Ghosts past this are too far to build to, and a size-128 shape reaches well beyond it. */
    public static final int VIEW = 48;
    /** A ceiling on the ghosts held in a plan. Past it the hologram closes in rather than thins out. */
    public static final int MAX_GHOSTS = 2000;
    /** Ticks between rechecks, so blocks laid into the hologram go out within a quarter second. */
    public static final int REFRESH = 5;

    private static final float R = 0.36F, G = 0.86F, B = 0.80F;
    /** Pulled in off the block faces so a ghost never z-fights the block it sits against. */
    private static final double INSET = 0.04;

    /** The worked-out plan: packed positions that still want a block, and what it was worked out for. */
    private static long[] ghosts = new long[MAX_GHOSTS];
    private static int count;
    private static int forShape = -1, forSize = -1, forRotation = -1;
    private static long forOrigin = Long.MIN_VALUE, builtAt = Long.MIN_VALUE;
    /** The world is remembered by name, never by reference: a static field holding a ClientLevel would
     *  keep every chunk and entity of a world you had already left alive for as long as the game ran. */
    private static String forDimension = "";

    /** The shape's offsets, kept for the one setting in hand. Rebuilding a size-128 dome's hundred
     *  thousand positions on every recheck would out-allocate everything else the client does. */
    private static BuildPattern cachedPattern;
    private static int cachedSize = -1;
    private static List<BlockPos> cachedOffsets = List.of();

    private ChalkGhostRenderer() {}

    /** Dropped on world change so a plan never survives into a level it was not worked out for. */
    public static void forget() {
        count = 0;
        forShape = forSize = forRotation = -1;
        forOrigin = Long.MIN_VALUE;
        builtAt = Long.MIN_VALUE;
        forDimension = "";
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.options.hideGui) {
            forget();
            return;
        }
        ItemStack stack = active(player);
        if (stack.isEmpty()) {
            forget();
            return;
        }
        BlockPos origin = origin(level, player, stack);
        if (origin == null) {
            forget();
            return;
        }

        plan(level, player, stack, origin);
        if (count == 0) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer faces = buffers.getBuffer(RenderType.debugFilledBox());
        for (int i = 0; i < count; i++) {
            long p = ghosts[i];
            float x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
            LevelRenderer.addChainedFilledBoxVertices(poses, faces,
                    (float) (x + INSET), (float) (y + INSET), (float) (z + INSET),
                    (float) (x + 1 - INSET), (float) (y + 1 - INSET), (float) (z + 1 - INSET),
                    R, G, B, 0.22F);
        }
        buffers.endBatch(RenderType.debugFilledBox());

        VertexConsumer edges = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i < count; i++) {
            long p = ghosts[i];
            double x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
            LevelRenderer.renderLineBox(poses, edges,
                    x + INSET, y + INSET, z + INSET, x + 1 - INSET, y + 1 - INSET, z + 1 - INSET,
                    R, G, B, 0.65F);
        }
        buffers.endBatch(RenderType.lines());

        poses.popPose();
    }

    /** Rebuild the standing plan when it no longer matches the chalk, the spot, or the world. */
    private static void plan(ClientLevel level, LocalPlayer player, ItemStack stack, BlockPos origin) {
        BuildPattern pattern = BuildersChalkItem.shape(stack);
        int scale = BuildersChalkItem.scale(stack);
        int rotation = BuildersChalkItem.rotation(stack, player).ordinal();
        long originKey = origin.asLong();
        long now = level.getGameTime();
        String dimension = level.dimension().location().toString();
        boolean stale = !forDimension.equals(dimension) || forShape != pattern.index() || forSize != scale
                || forRotation != rotation || forOrigin != originKey || now - builtAt >= REFRESH || now < builtAt;
        if (!stale) return;
        forDimension = dimension;
        forShape = pattern.index();
        forSize = scale;
        forRotation = rotation;
        forOrigin = originKey;
        builtAt = now;

        List<BlockPos> offsets = offsets(pattern, scale, Rotation.values()[rotation]);
        BlockPos eye = Minecraft.getInstance().player.blockPosition();
        int reach = VIEW;
        count = gather(level, offsets, origin, eye, reach);
        // Too much to draw at once: close in rather than thin out, so what is shown stays solid.
        while (count >= MAX_GHOSTS && reach > 8) {
            reach /= 2;
            count = gather(level, offsets, origin, eye, reach);
        }
    }

    private static int cachedRotation = -1;

    private static List<BlockPos> offsets(BuildPattern pattern, int size, Rotation rotation) {
        if (pattern != cachedPattern || size != cachedSize || rotation.ordinal() != cachedRotation) {
            List<BlockPos> raw = pattern.offsets(size);
            if (rotation == Rotation.NONE) cachedOffsets = raw;
            else {
                List<BlockPos> turned = new ArrayList<>(raw.size());
                for (BlockPos offset : raw) turned.add(BuildPattern.turn(offset, rotation));
                cachedOffsets = List.copyOf(turned);
            }
            cachedPattern = pattern;
            cachedSize = size;
            cachedRotation = rotation.ordinal();
        }
        return cachedOffsets;
    }

    /** Collects the still-open positions into {@link #ghosts}, stopping at the ceiling. */
    private static int gather(ClientLevel level, List<BlockPos> offsets, BlockPos origin, BlockPos eye, int reach) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int found = 0;
        for (BlockPos offset : offsets) {
            int y = origin.getY() + offset.getY();
            if (Math.abs(y - eye.getY()) > reach) continue;
            int x = origin.getX() + offset.getX();
            if (Math.abs(x - eye.getX()) > reach) continue;
            int z = origin.getZ() + offset.getZ();
            if (Math.abs(z - eye.getZ()) > reach) continue;
            at.set(x, y, z);
            if (!level.isLoaded(at) || !level.getBlockState(at).canBeReplaced()) continue;
            ghosts[found++] = at.asLong();
            if (found >= MAX_GHOSTS) break;
        }
        return found;
    }

    /**
     * The chalk whose layout should be standing.
     *
     * <p>In hand it always draws. Set down — its mark driven into a spot — it keeps drawing from
     * anywhere in the hotbar, which is the whole point: you put the chalk away, take up blocks, and
     * build inside the hologram it left behind. A chalk with no mark is ignored unless it is held,
     * because a layout that trailed your feet while you swung a pickaxe would only be in the way.
     */
    private static ItemStack active(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof BuildersChalkItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof BuildersChalkItem) return off;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BuildersChalkItem && BuildersChalkItem.mark(stack) != null) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The set mark when there is one in this dimension and in range, otherwise the holder's feet. */
    private static BlockPos origin(ClientLevel level, LocalPlayer player, ItemStack stack) {
        BlockPos marked = BuildersChalkItem.mark(stack);
        if (marked == null) return player.blockPosition();
        if (!BuildersChalkItem.markDimension(stack).equals(level.dimension().location().toString())) return null;
        return marked.closerThan(player.blockPosition(), BuildersChalkItem.ANCHOR_RANGE) ? marked : null;
    }
}
