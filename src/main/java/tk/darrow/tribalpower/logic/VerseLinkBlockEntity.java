package tk.darrow.tribalpower.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Sixteen verses. A Call sings what it hears; an Answer within 32 repeats that song. */
public class VerseLinkBlockEntity extends BlockEntity {
    public static final int RANGE = 32;
    private int verse;
    private int heard;

    public VerseLinkBlockEntity(BlockPos pos, BlockState state) {
        super(LogicRegistry.VERSE_LINK_TYPE.get(), pos, state);
    }

    public int verse() { return verse; }
    public int heard() { return heard; }

    public int cycle() {
        verse = (verse + 1) % 16;
        setChanged();
        return verse + 1;
    }

    public boolean call() {
        return getBlockState().getBlock() instanceof VerseLinkBlock link && link.call();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (call() && level instanceof ServerLevel server) VerseCalls.add(server, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (call() && level instanceof ServerLevel server) VerseCalls.remove(server, worldPosition);
        super.setRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VerseLinkBlockEntity be) {
        if (level.isClientSide) return;
        if (be.call() && level instanceof ServerLevel server) VerseCalls.add(server, pos);
        int next = be.call() ? listenLocal(level, pos, state.getValue(VerseLinkBlock.FACING))
                : hear((ServerLevel) level, pos, be.verse);
        be.heard = next;
        if (state.getValue(VerseLinkBlock.POWER) != next) {
            BlockState nextState = state.setValue(VerseLinkBlock.POWER, next);
            level.setBlock(pos, nextState, 3);
            if (!be.call()) {
                Direction facing = state.getValue(VerseLinkBlock.FACING);
                level.updateNeighborsAt(pos.relative(facing), state.getBlock());
            }
            be.setChanged();
        }
    }

    private static int listenLocal(Level level, BlockPos pos, Direction facing) {
        int best = 0;
        for (Direction side : Direction.values()) {
            if (side == facing) continue;
            BlockPos neighbor = pos.relative(side);
            if (level.getBlockState(neighbor).getBlock() instanceof VerseLinkBlock) continue;
            best = Math.max(best, level.getSignal(neighbor, side));
        }
        return Math.clamp(best, 0, 15);
    }

    public static int hear(ServerLevel level, BlockPos listener, int verse) {
        return VerseCalls.hear(level, listener, verse);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Verse", verse);
        tag.putInt("Heard", heard);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        verse = Math.clamp(tag.getInt("Verse"), 0, 15);
        heard = Math.clamp(tag.getInt("Heard"), 0, 15);
    }
}
