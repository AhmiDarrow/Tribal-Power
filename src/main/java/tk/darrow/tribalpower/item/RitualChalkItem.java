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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.List;
import java.util.UUID;

/**
 * The chalk does two jobs.
 *
 * <p>On a Resonance Totem it marks lasting Totem Lattice links: first click stores a totem, second click
 * within range seals the link.
 *
 * <p>Anywhere else it draws a Ritual Mark on the ground -- the thing that turns scattered devices into a
 * rite (design 3.1 section 7.1). Clicking a drawn mark rubs it out. Each stick holds ten marks or links;
 * a craft still makes four sticks. Rubbing a mark out costs nothing.
 */
public class RitualChalkItem extends Item {
    public static final int USES = 10;
    private static final String TAG_USES = "Uses";
    private static final String TAG_STICK = "Stick";
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
            return drawMark(context);
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
        if (linked) spend(context);
        return InteractionResult.SUCCESS;
    }

    /**
     * Draws a Ritual Mark on the floor, or rubs out the one already there. Marks are what every pattern
     * that claims to be a rite is joined with, so without this the Stone Font, the Listening Pit and the
     * Rite Circle cannot be built at all.
     */
    private static InteractionResult drawMark(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        // A mark is replaceable, so clicking one directly lands on it; clicking the floor lands beside it.
        BlockPos target = level.getBlockState(clicked).canBeReplaced()
                ? clicked : clicked.relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block mark = ModBlocks.RITUAL_MARK.get();

        if (level.getBlockState(target).is(mark)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            level.destroyBlock(target, false);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.tribalpower.chalk.erased"), true);
            }
            return InteractionResult.SUCCESS;
        }

        BlockState state = mark.defaultBlockState();
        if (!level.getBlockState(target).canBeReplaced() || !state.canSurvive(level, target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        level.setBlock(target, state, Block.UPDATE_ALL);
        tk.darrow.tribalpower.sound.ModSounds.play(level, target,
                tk.darrow.tribalpower.sound.ModSounds.CHALK_DRAW, 0.7F, 1.05F);
        spend(context);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.chalk.drawn"), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static void spend(UseOnContext context) {
        spend(context.getItemInHand(), context.getPlayer());
    }

    /** One mark or link from a stick of ten. Creative play is free. */
    public static void spend(ItemStack stack, Player player) {
        if (player != null && player.getAbilities().instabuild) return;
        if (stack.getCount() > 1) {
            ItemStack one = stack.split(1);
            consumeUse(one);
            if (one.isEmpty()) return;
            if (player != null) {
                if (!player.getInventory().add(one)) player.drop(one, false);
            }
            return;
        }
        consumeUse(stack);
    }

    private static void consumeUse(ItemStack stack) {
        int left = remaining(stack) - 1;
        if (left <= 0) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.remove(TAG_USES);
            tag.remove(TAG_STICK);
            if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
            else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            stack.shrink(1);
        } else {
            writeUses(stack, left);
        }
    }

    public static int remaining(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return USES;
        CompoundTag tag = data.copyTag();
        return tag.contains(TAG_USES) ? Math.clamp(tag.getInt(TAG_USES), 0, USES) : USES;
    }

    private static void writeUses(ItemStack stack, int uses) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_USES, uses);
        if (!tag.hasUUID(TAG_STICK)) tag.putUUID(TAG_STICK, UUID.randomUUID());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return remaining(stack) < USES;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * remaining(stack) / USES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float t = remaining(stack) / (float) USES;
        return net.minecraft.util.Mth.hsvToRgb(t / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.ritual_chalk.desc"));
        tooltip.add(Component.translatable("item.tribalpower.ritual_chalk.marks"));
        tooltip.add(Component.translatable("item.tribalpower.ritual_chalk.uses", remaining(stack), USES));
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
