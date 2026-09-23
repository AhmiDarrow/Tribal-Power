package tk.darrow.tribalpower.building;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

/**
 * Builder's Chalk: a builder's guide. Pick a shape, set its size, and the chalk stands a hologram of it
 * in the world — a ghost block in every space the shape still needs — so you can lay real blocks inside
 * the ghosts and watch the outline empty out as you go.
 *
 * <p>Unlike Ritual Chalk it is never spent. It places nothing and breaks nothing: this class only keeps
 * the setting on the stack, and {@link ChalkGhostRenderer} draws from that setting on the holder's own
 * client. Nothing is sent to the server for it and no other player sees it.
 *
 * <p>Past the building shapes the same stick stands the placement rites. The mark is the machine, and
 * sneaking steps the tier instead of the size, so a Listening Pit can be the first layout or the deep one
 * without losing the hut you had sized a moment ago.
 */
public class BuildersChalkItem extends Item {
    /** How far from a set mark the hologram still stands. */
    public static final int ANCHOR_RANGE = 192;
    private static final String SHAPE = "ChalkShape", SIZE = "ChalkSize", TIER = "ChalkTier",
            ROT = "ChalkRot", MARK = "ChalkMark", DIM = "ChalkDim";

    public BuildersChalkItem(Properties properties) {
        super(properties);
    }

    // ---- stack state -------------------------------------------------------------------------------

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static BuildPattern shape(ItemStack stack) {
        return stack.isEmpty() ? BuildPattern.SQUARE : BuildPattern.byIndex(data(stack).getInt(SHAPE));
    }

    public static int size(ItemStack stack) {
        if (stack.isEmpty()) return BuildPattern.MIN_SIZE;
        CompoundTag tag = data(stack);
        return BuildPattern.clampSize(tag.contains(SIZE) ? tag.getInt(SIZE) : 5);
    }

    /** The rite tier on the stack, or 1 for a building shape and for a tier the rite does not have. */
    public static int tier(ItemStack stack) {
        var rite = shape(stack).rite();
        if (rite == null) return 1;
        int stored = data(stack).getInt(TIER);
        return rite.tier(stored) == null ? 1 : stored;
    }

    /** Half-extent for a building shape, tier number for a rite. What the hologram is drawn from. */
    public static int scale(ItemStack stack) {
        return shape(stack).rite() == null ? size(stack) : tier(stack);
    }

    /**
     * How a rite is turned. A set mark keeps the facing it was planted with. While the layout follows
     * you, it turns to the way you are looking, and a building shape never turns.
     */
    public static Rotation rotation(ItemStack stack, Player player) {
        if (shape(stack).rite() == null) return Rotation.NONE;
        if (mark(stack) != null) {
            int stored = data(stack).getInt(ROT);
            Rotation[] all = Rotation.values();
            if (stored >= 0 && stored < all.length) return all[stored];
        }
        return facing(player.getDirection());
    }

    /** Pattern +Z, the way the rite was written, swings around to this horizontal facing. */
    public static Rotation facing(Direction direction) {
        return switch (direction) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Where the chalk has set its mark, or null while the hologram follows the holder. */
    public static BlockPos mark(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.contains(MARK) ? BlockPos.of(tag.getLong(MARK)) : null;
    }

    /** The dimension the mark was set in, so a hologram never hangs in the wrong world. */
    public static String markDimension(ItemStack stack) {
        return data(stack).getString(DIM);
    }

    private static void cycleShape(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SHAPE, shape(stack).index() + 1));
    }

    private static void stepSize(ItemStack stack, boolean up) {
        int next = up ? BuildPattern.nextSize(size(stack)) : BuildPattern.previousSize(size(stack));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SIZE, next));
    }

    /** One tier up or down, wrapping at either end. A single-tier rite stays where it is. */
    private static void stepTier(ItemStack stack, boolean up) {
        var rite = shape(stack).rite();
        int max = rite.maxTier();
        int current = tier(stack);
        int next = up ? (current >= max ? 1 : current + 1) : (current <= 1 ? max : current - 1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TIER, next));
    }

    private static void setMark(ItemStack stack, BlockPos pos, String dimension, Direction facing) {
        int rotation = facing(facing).ordinal();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(MARK, pos.asLong());
            tag.putString(DIM, dimension);
            tag.putInt(ROT, rotation);
        });
    }

    private static void clearMark(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(MARK);
            tag.remove(DIM);
        });
    }

    // ---- interactions ------------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                resize(player, stack, true);
            } else {
                cycleShape(stack);
                player.displayClientMessage(setting(stack), true);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS, 0.4F, 1.0F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                resize(player, stack, true);
                return InteractionResult.SUCCESS;
            }
            BlockPos target = context.getClickedPos().above();
            BlockPos current = mark(stack);
            boolean sameSpot = current != null && current.equals(target)
                    && markDimension(stack).equals(level.dimension().location().toString());
            if (sameSpot) {
                clearMark(stack);
                player.displayClientMessage(Component.translatable("message.tribalpower.chalk.rubbed"), true);
            } else {
                setMark(stack, target, level.dimension().location().toString(), player.getDirection());
                player.displayClientMessage(Component.translatable("message.tribalpower.chalk.marked",
                        target.getX(), target.getY(), target.getZ()), true);
            }
            level.playSound(null, target, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 0.7F, sameSpot ? 1.3F : 0.9F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Shift and a left click steps the size back down, and eats the click so the chalk never knocks a
     * hole in what you are building. Only the opening click counts: the event keeps firing while the
     * button is held, and a size that ran away under your finger would be unusable.
     */
    public static void shrink(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BuildersChalkItem) || !player.isShiftKeyDown()) return;
        event.setCanceled(true);
        if (!event.getLevel().isClientSide) resize(player, stack, false);
    }

    private static void resize(Player player, ItemStack stack, boolean up) {
        if (shape(stack).rite() != null) stepTier(stack, up);
        else stepSize(stack, up);
        player.displayClientMessage(setting(stack), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.4F, up ? 1.4F : 0.7F);
    }

    // ---- readouts ----------------------------------------------------------------------------------

    public static Component setting(ItemStack stack) {
        BuildPattern shape = shape(stack);
        if (shape.rite() != null)
            return Component.translatable("message.tribalpower.chalk.rite",
                    Component.translatable(shape.key()), tier(stack));
        return Component.translatable("message.tribalpower.chalk.setting",
                Component.translatable(shape.key()), size(stack), size(stack) * 2 + 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(setting(stack).copy().withStyle(ChatFormatting.AQUA));
        BlockPos marked = mark(stack);
        tooltip.add((marked == null
                ? Component.translatable("gui.tribalpower.chalk.follows")
                : Component.translatable("gui.tribalpower.chalk.marked_at",
                        marked.getX(), marked.getY(), marked.getZ())).withStyle(ChatFormatting.DARK_AQUA));
        if (marked != null)
            tooltip.add(Component.translatable("gui.tribalpower.chalk.stays").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable(shape(stack).rite() == null
                ? "gui.tribalpower.chalk.hint" : "gui.tribalpower.chalk.rite_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("gui.tribalpower.chalk.never_spent").withStyle(ChatFormatting.DARK_GRAY));
    }
}
