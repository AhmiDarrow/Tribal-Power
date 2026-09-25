package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.block.CampDisplay;
import tk.darrow.tribalpower.blockentity.CampDisplayBlockEntity;

/**
 * Draws what has been set down on camp furniture: a shelf's board, a table's top, an urn's mouth, a rack's hooks.
 *
 * <p>Generic over the block entity so anything that is display plus something else can use it. The
 * Tribal Bench is display plus a crafting grid, and has its own block entity type; bound to the
 * camp display type alone, its shelf held items that nobody could see.
 */
public class CampDisplayRenderer<T extends CampDisplayBlockEntity> implements BlockEntityRenderer<T> {
    private static final float SCALE = 0.4F;

    private final ItemRenderer itemRenderer;

    public CampDisplayRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T display, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = display.getBlockState();
        float yaw = -CampDisplay.facingOf(state).toYRot();

        for (int slot = 0; slot < display.capacity(); slot++) {
            ItemStack stack = display.at(slot);
            if (stack.isEmpty()) continue;

            Vec3 where = CampDisplay.offsetOf(state, slot);
            pose.pushPose();
            pose.translate(where.x, where.y, where.z);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            // A board or a table top lays things flat; an urn stands one upright in its mouth.
            if (CampDisplay.laysFlat(state)) pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            if (CampDisplay.hangs(state)) {
                // Pivot on the hook and swing a little along the rail, each hook out of step with the next,
                // then drop the item so its top sits on the hook.
                float time = display.getLevel() == null ? 0F : display.getLevel().getGameTime() + partialTick;
                long seed = display.getBlockPos().asLong();
                pose.mulPose(Axis.ZP.rotationDegrees(2.5F * net.minecraft.util.Mth.sin(time * 0.045F + slot * 1.7F + (seed & 63))));
                pose.translate(0.0F, -SCALE / 2.0F, 0.0F);
            }
            pose.scale(SCALE, SCALE, SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                    display.getLevel(), (int) display.getBlockPos().asLong());
            pose.popPose();
        }
    }
}
