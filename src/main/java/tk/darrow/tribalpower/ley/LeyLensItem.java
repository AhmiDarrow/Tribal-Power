package tk.darrow.tribalpower.ley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;

import java.util.List;

/**
 * Ley Lens: held in either hand it shows "Ley: NN%" on the HUD (client, {@link LeyLensHud}) and every
 * 10 ticks the server paints a 9×9 grid of ground particles coloured blue (weak) to gold (strong).
 * Sneak-use on a Ley Collector prints that collector's exact factor breakdown.
 */
public class LeyLensItem extends Item {
    public static final int GRID = 4;
    public static final int INTERVAL = 10;
    private static final Vector3f WEAK = new Vector3f(0.25F, 0.45F, 1.0F);
    private static final Vector3f STRONG = new Vector3f(1.0F, 0.82F, 0.25F);

    public LeyLensItem(Properties properties) {
        super(properties);
    }

    public static boolean holding(Entity entity) {
        return entity instanceof net.minecraft.world.entity.LivingEntity living
                && (living.getMainHandItem().getItem() instanceof LeyLensItem || living.getOffhandItem().getItem() instanceof LeyLensItem);
    }

    public static Vector3f colour(double strength) {
        float t = (float) Math.max(0, Math.min(1, strength));
        return new Vector3f(WEAK).lerp(STRONG, t);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return;
        if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) return;
        if (server.getGameTime() % INTERVAL != 0) return;
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -GRID; dx <= GRID; dx++) {
            for (int dz = -GRID; dz <= GRID; dz++) {
                cursor.set(origin.getX() + dx, origin.getY() + 1, origin.getZ() + dz);
                int ground = groundY(server, cursor, 6);
                if (ground == Integer.MIN_VALUE) continue;
                cursor.setY(ground + 1);
                double strength = LeyMath.strength(server, cursor);
                DustParticleOptions dust = new DustParticleOptions(colour(strength), 0.6F + (float) strength * 0.6F);
                server.sendParticles(player, dust, false, cursor.getX() + 0.5, ground + 1.08, cursor.getZ() + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Y of the first block with a solid top below {@code from}, scanning at most {@code depth} blocks; MIN_VALUE if none. */
    private static int groundY(Level level, BlockPos from, int depth) {
        BlockPos.MutableBlockPos cursor = from.mutable();
        for (int i = 0; i <= depth; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty()) return cursor.getY();
            cursor.move(0, -1, 0);
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LeyCollectorBlockEntity collector)) return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel server) {
            BlockPos pos = context.getClickedPos();
            player.sendSystemMessage(Component.translatable("ley.tribalpower.header", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA));
            for (Component line : LeyMath.breakdown(server, pos))
                player.sendSystemMessage(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA).append(line.copy().withStyle(ChatFormatting.GRAY)));
            player.sendSystemMessage(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.translatable("diag.tribalpower.stored", collector.getPulseStored(), collector.getPulseCapacity()).withStyle(ChatFormatting.GRAY)));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.ley_lens.desc").withStyle(ChatFormatting.GRAY));
    }
}
