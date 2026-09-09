package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;

public class DrumheartBlockEntity extends BlockEntity implements PulseHandler, tk.darrow.tribalpower.api.Diagnosable {
    public static final int CAPACITY = 1000;
    public static final int BEAT_GAIN = 10;
    public static final int REDSTONE_GAIN = 5;

    private final PulseStorage pulse = new PulseStorage(CAPACITY);
    private int redstoneCooldown;
    private long lastManualBeat = -100;

    public DrumheartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRUMHEART.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrumheartBlockEntity be) {
        if (be.redstoneCooldown > 0) {
            be.redstoneCooldown--;
        }
    }

    public int drumBeat() {
        long now = level == null ? 0 : level.getGameTime();
        long interval = now - lastManualBeat;
        if (interval < 8) return 0;
        boolean inTime = interval >= 17 && interval <= 23;
        lastManualBeat = now;
        int gained = insertPulse(inTime ? 24 : BEAT_GAIN, false);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            tk.darrow.tribalpower.effect.SpiritEffects.ring(server, worldPosition.getCenter().add(0, 0.4, 0),
                    tk.darrow.tribalpower.api.pulse.Attunement.EARTH, inTime ? 1 : 0.55, inTime ? 16 : 8);
            server.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.65F, inTime ? 1.3F : 0.9F);
        }
        setChanged();
        return gained;
    }

    public void onRedstonePulse() {
        if (redstoneCooldown > 0) {
            return;
        }
        redstoneCooldown = 8;
        insertPulse(REDSTONE_GAIN, false);
        setChanged();
    }

    @Override
    public int getPulseStored() {
        return pulse.getPulseStored();
    }

    @Override
    public int getPulseCapacity() {
        return pulse.getPulseCapacity();
    }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int n = pulse.insertPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        int n = pulse.extractPulse(amount, simulate);
        if (!simulate && n > 0) {
            setChanged();
        }
        return n;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
        tag.putInt("RedstoneCooldown", redstoneCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        redstoneCooldown = net.minecraft.util.Mth.clamp(tag.getInt("RedstoneCooldown"), 0, 8);
    }

    @Override public java.util.List<net.minecraft.network.chat.Component> diagnose(net.minecraft.server.level.ServerLevel server, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        long since = server.getGameTime() - lastManualBeat;
        lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.drumheart.beat", since < 200 ? Long.toString(since) : "-", BEAT_GAIN, REDSTONE_GAIN, redstoneCooldown));
        if (!canReceivePulse()) lines.add(net.minecraft.network.chat.Component.translatable("diag.tribalpower.output_full").withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }
}
