package tk.darrow.tribalpower.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stateful song-plate logic. Binary gates read left and right of FACING; unary gates read the back. */
public class LogicPlateBlockEntity extends BlockEntity {
    private int memory;
    private int delay;
    private int period = 20;
    private int wait = 4;
    private int threshold = 4;
    private int step;
    private boolean lastLeft;
    private boolean lastBack;
    private String reason = "Listening";

    public LogicPlateBlockEntity(BlockPos pos, BlockState state) {
        super(LogicRegistry.PLATE_TYPE.get(), pos, state);
    }

    public LogicKind kind() {
        if (getBlockState().getBlock() instanceof LogicPlateBlock plate) return plate.kind();
        return LogicKind.fromId(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath());
    }

    public String status() { return reason; }

    public String cycle() {
        String next = switch (kind()) {
            case HEARTBEAT -> {
                period = period >= 160 ? 20 : period * 2;
                yield "Heartbeat every " + period + " ticks";
            }
            case DRIFT -> {
                wait = wait >= 32 ? 4 : wait * 2;
                yield "Drift " + wait + " ticks";
            }
            case TALLY -> {
                threshold = threshold >= 8 ? 1 : threshold + 1;
                yield "Tally " + threshold;
            }
            default -> reason;
        };
        setChanged();
        return next;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LogicPlateBlockEntity be) {
        if (level.isClientSide) return;
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        int left = input(level, pos, leftOf(facing));
        int right = input(level, pos, rightOf(facing));
        int back = input(level, pos, facing.getOpposite());
        boolean leftOn = left > 0, rightOn = right > 0, backOn = back > 0;
        int out = 0;
        LogicKind kind = be.kind();
        switch (kind) {
            case CHORUS -> { out = leftOn && rightOn ? 15 : 0; be.reason = "Chorus " + (out > 0 ? "sings" : "waits"); }
            case GATHERING -> { out = leftOn || rightOn ? 15 : 0; be.reason = "Gathering " + (out > 0 ? "open" : "quiet"); }
            case DISCORD -> { out = leftOn ^ rightOn ? 15 : 0; be.reason = "Discord " + (out > 0 ? "split" : "even"); }
            case HUSH -> { out = !(leftOn && rightOn) ? 15 : 0; be.reason = "Hush " + (out > 0 ? "holds" : "breaks"); }
            case INVERSE -> { out = backOn ? 0 : 15; be.reason = "Inverse " + (out > 0 ? "flipped" : "pressed"); }
            case ECHO -> { out = backOn ? 15 : 0; be.reason = "Echo " + (out > 0 ? "carries" : "silent"); }
            case MEMORY -> {
                if (leftOn) be.memory = 15;
                if (rightOn) be.memory = 0;
                out = be.memory;
                be.reason = out > 0 ? "Memory set" : "Memory clear";
            }
            case HEARTBEAT -> {
                be.delay++;
                if (be.delay >= be.period) { be.delay = 0; out = 15; }
                be.reason = "Heartbeat " + be.period + "t";
            }
            case DRIFT -> {
                if (backOn) { if (be.delay < be.wait) be.delay++; }
                else be.delay = 0;
                out = be.delay >= be.wait ? 15 : 0;
                be.reason = "Drift " + be.delay + "/" + be.wait;
            }
            case TALLY -> {
                if (backOn && !be.lastBack) be.memory = Math.min(15, be.memory + 1);
                if (leftOn && !be.lastLeft) be.memory = 0;
                out = be.memory >= be.threshold ? 15 : 0;
                be.reason = "Tally " + be.memory + "/" + be.threshold;
            }
            case STRIKE -> {
                out = backOn && !be.lastBack ? 15 : 0;
                be.reason = out > 0 ? "Strike" : "Waiting for a rising edge";
            }
            case CHANCE -> {
                if (backOn && !be.lastBack) be.memory = level.random.nextBoolean() ? 15 : 0;
                if (!backOn) be.memory = 0;
                out = be.memory;
                be.reason = "Chance " + (out > 0 ? "yes" : "no");
            }
            case VERSE -> {
                if (backOn && !be.lastBack) be.step = (be.step + 1) % 4;
                out = new int[]{4, 8, 12, 15}[be.step];
                be.reason = "Verse step " + (be.step + 1);
            }
        }
        be.lastBack = backOn;
        be.lastLeft = leftOn;
        if (state.getValue(LogicPlateBlock.POWER) != out) {
            level.setBlock(pos, state.setValue(LogicPlateBlock.POWER, out), 3);
            level.updateNeighborsAt(pos.relative(facing), state.getBlock());
            be.setChanged();
        }
    }

    private static int input(Level level, BlockPos pos, Direction side) {
        BlockPos neighbor = pos.relative(side);
        BlockState other = level.getBlockState(neighbor);
        if (other.getBlock() instanceof LogicPlateBlock && other.getValue(LogicPlateBlock.FACING) == side.getOpposite()) {
            return 0;
        }
        return Math.max(level.getSignal(neighbor, side), other.getDirectSignal(level, neighbor, side.getOpposite()));
    }

    private static Direction leftOf(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            case UP, DOWN -> Direction.WEST;
        };
    }

    private static Direction rightOf(Direction facing) {
        return leftOf(facing).getOpposite();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Memory", memory);
        tag.putInt("Delay", delay);
        tag.putInt("Period", period);
        tag.putInt("Wait", wait);
        tag.putInt("Threshold", threshold);
        tag.putInt("Step", step);
        tag.putBoolean("LastLeft", lastLeft);
        tag.putBoolean("LastBack", lastBack);
        tag.putString("Reason", reason);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        memory = tag.getInt("Memory");
        delay = tag.getInt("Delay");
        period = Math.max(20, tag.getInt("Period"));
        wait = Math.max(4, tag.getInt("Wait"));
        threshold = Math.max(1, tag.getInt("Threshold"));
        step = tag.getInt("Step");
        lastLeft = tag.getBoolean("LastLeft");
        lastBack = tag.getBoolean("LastBack");
        reason = tag.getString("Reason");
    }
}
