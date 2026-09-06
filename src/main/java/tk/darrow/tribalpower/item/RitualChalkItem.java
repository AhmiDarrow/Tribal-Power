package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.List;

/**
 * Marks lasting Totem Lattice links. First click stores a totem; second click within range seals the link.
 */
public class RitualChalkItem extends Item {
    private static final String TAG_LINK = "LatticePending";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";

    public RitualChalkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResonanceTotemBlockEntity)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pending = readPending(stack);
        if (pending == null) {
            writePending(stack, pos);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.tribalpower.chalk.mark"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (pending.equals(pos)) {
            clearPending(stack);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.tribalpower.chalk.clear"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!LatticeNetwork.canLink(pending, pos)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.tribalpower.chalk.too_far"), true);
            }
            return InteractionResult.FAIL;
        }

        BlockEntity other = level.getBlockEntity(pending);
        if (!(other instanceof ResonanceTotemBlockEntity fromTotem)
                || !(be instanceof ResonanceTotemBlockEntity toTotem)) {
            clearPending(stack);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.tribalpower.chalk.fail"), true);
            }
            return InteractionResult.FAIL;
        }

        boolean linked = LatticeNetwork.linkTotems(fromTotem, toTotem);
        clearPending(stack);
        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    linked ? "message.tribalpower.chalk.link" : "message.tribalpower.chalk.already"
            ), true);
        }
        if (linked && player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.ritual_chalk.desc"));
        BlockPos pending = readPending(stack);
        if (pending != null) {
            tooltip.add(Component.translatable(
                    "item.tribalpower.ritual_chalk.pending",
                    pending.getX(), pending.getY(), pending.getZ()
            ));
        }
    }

    private static BlockPos readPending(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_LINK)) {
            return null;
        }
        CompoundTag link = tag.getCompound(TAG_LINK);
        return new BlockPos(link.getInt(TAG_X), link.getInt(TAG_Y), link.getInt(TAG_Z));
    }

    private static void writePending(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag link = new CompoundTag();
        link.putInt(TAG_X, pos.getX());
        link.putInt(TAG_Y, pos.getY());
        link.putInt(TAG_Z, pos.getZ());
        tag.put(TAG_LINK, link);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearPending(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(TAG_LINK);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
