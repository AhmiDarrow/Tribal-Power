package tk.darrow.tribalpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tk.darrow.tribalpower.blockentity.CampDisplayBlockEntity;

/**
 * Setting things down on camp furniture and picking them up again. The Wall Shelf, Hanging Rack, March
 * Table and Spirit Urn all behave the same way — use with something in hand to set it down at the place you
 * clicked, use empty-handed to take it back — so the behaviour lives here rather than in each block.
 */
public final class CampDisplay {
    private CampDisplay() {}

    /**
     * Which place was clicked. A shelf's four run left to right along its board; a table's four are
     * the quarters of its top; an urn has one.
     */
    public static int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
        int capacity = CampDisplayBlockEntity.capacityOf(state);
        if (capacity <= 1) return 0;

        double x = hit.x - pos.getX();
        double z = hit.z - pos.getZ();

        if (state.getBlock() instanceof WallShelfBlock || state.getBlock() instanceof TribalBenchBlock) {
            double along = switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
                case NORTH -> x;
                case SOUTH -> 1.0 - x;
                case WEST -> 1.0 - z;
                default -> z;
            };
            return Math.clamp((int) (along * capacity), 0, capacity - 1);
        }

        // A table top: near/far and left/right make the four quarters.
        int half = x < 0.5 ? 0 : 1;
        int row = z < 0.5 ? 0 : 1;
        return Math.clamp(row * 2 + half, 0, capacity - 1);
    }

    /** Sets one item down. Returns PASS when there is no room, so the block can fall through. */
    public static ItemInteractionResult place(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CampDisplayBlockEntity display) || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = display.freeSlotFrom(slotAt(state, pos, hit.getLocation()));
        // Full: let the held item do its own thing (place a block against it), but never fall through to taking one.
        if (slot < 0) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack left = display.place(slot, stack.copy());
            if (!player.isCreative()) player.setItemInHand(hand, left);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Takes the nearest item back. */
    public static InteractionResult take(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CampDisplayBlockEntity display)) return InteractionResult.PASS;
        int slot = display.filledSlotFrom(slotAt(state, pos, hit.getLocation()));
        if (slot < 0) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = display.take(slot);
            tk.darrow.tribalpower.item.SpiritgearHelper.give(player, taken);
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.7F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** What is set down on a piece falls with it. */
    public static void dropContents(BlockState state, Level level, BlockPos pos, BlockState newState, Block block) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CampDisplayBlockEntity display) {
            Containers.dropContents(level, pos, display);
            level.updateNeighbourForOutputSignal(pos, block);
        }
    }

    public static int signal(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CampDisplayBlockEntity display ? display.signal() : 0;
    }

    /** Where a place sits inside the block, for the renderer. */
    public static Vec3 offsetOf(BlockState state, int slot) {
        if (state.is(ModBlocks.SPIRIT_URN.get())) {
            return new Vec3(0.5, 14.0 / 16.0, 0.5);                       // the mouth of the pot
        }
        if (state.getBlock() instanceof WallShelfBlock || state.getBlock() instanceof TribalBenchBlock) {
            int capacity = CampDisplayBlockEntity.capacityOf(state);
            float along = (slot + 0.5F) / capacity;
            float depth = 3.0F / 16.0F;
            if (state.getBlock() instanceof HangingRackBlock) {
                // The hook's tip, out from the wall under the rail; the renderer hangs the item down from here.
                float d = 5.0F / 16.0F, y = HOOK_Y;
                return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
                    case NORTH -> new Vec3(along, y, 1.0 - d);
                    case SOUTH -> new Vec3(1.0 - along, y, d);
                    case WEST -> new Vec3(1.0 - d, y, 1.0 - along);
                    default -> new Vec3(d, y, along);
                };
            }
            if (state.getBlock() instanceof TribalBenchBlock) {
                // The bench board stands above its top, not against a wall, so it sits higher.
                float d = 4.0F / 16.0F;
                return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
                    case NORTH -> new Vec3(along, 1.0, 1.0 - d);
                    case SOUTH -> new Vec3(1.0 - along, 1.0, d);
                    case WEST -> new Vec3(1.0 - d, 1.0, 1.0 - along);
                    default -> new Vec3(d, 1.0, along);
                };
            }
            return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
                case NORTH -> new Vec3(along, 9.0 / 16.0, 1.0 - depth);
                case SOUTH -> new Vec3(1.0 - along, 9.0 / 16.0, depth);
                case WEST -> new Vec3(1.0 - depth, 9.0 / 16.0, 1.0 - along);
                default -> new Vec3(depth, 9.0 / 16.0, along);
            };
        }
        // Table top, quarters, sitting on the 16/16 surface.
        double x = (slot % 2 == 0) ? 0.3 : 0.7;
        double z = (slot / 2 == 0) ? 0.3 : 0.7;
        return new Vec3(x, 1.0, z);
    }

    /** Where a Hanging Rack's hooks end, as a fraction of the block's height. */
    public static final float HOOK_Y = 10.25F / 16.0F;

    /** Camp furniture lays items flat; the urn stands one up in its mouth and the rack hangs them. */
    public static boolean laysFlat(BlockState state) {
        return !state.is(ModBlocks.SPIRIT_URN.get()) && !hangs(state);
    }

    /** Items hang down from a Hanging Rack's hooks rather than resting on something. */
    public static boolean hangs(BlockState state) {
        return state.getBlock() instanceof HangingRackBlock;
    }

    public static Direction facingOf(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }
}
