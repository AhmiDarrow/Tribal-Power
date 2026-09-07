package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.List;

/**
 * Routes Spirit Pulse along chalk-linked Resonance Totems and assists Song Benches / caches on the lattice.
 */
public class LatticeConductorBlockEntity extends BlockEntity {
    public static final int RADIUS = LatticeNetwork.DEFAULT_RADIUS;
    public static final int TICK_INTERVAL = 20;
    public static final int ITEM_INTERVAL = 40;
    public static final int PUSH_PER_CYCLE = 10;
    public static final int CLICK_PUSH = 25;

    private int tickCounter;
    private int networkSize;
    private int lastPulsePushed;
    private boolean assistActive;
    private boolean lastItemRouted;

    public LatticeConductorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LATTICE_CONDUCTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LatticeConductorBlockEntity be) {
        if (level.hasNeighborSignal(pos)) return;
        be.tickCounter++;
        if (be.tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        List<ResonanceTotemBlockEntity> network = LatticeNetwork.collectChalkNetworkNear(level, pos, RADIUS);
        be.networkSize = network.size();
        if (!LatticeNetwork.isConductable(network)) {
            be.assistActive = false;
            be.lastPulsePushed = 0;
            be.lastItemRouted = false;
            be.setChanged();
            return;
        }

        List<BlockPos> hubs = LatticeNetwork.networkHubs(network);
        List<SongBenchBlockEntity> benches = LatticeNetwork.findSongBenchesNearHubs(level, hubs, RADIUS);
        be.assistActive = false;
        for (SongBenchBlockEntity bench : benches) {
            if (bench.wantsPulseAssist()) {
                be.assistActive = true;
                break;
            }
        }

        int available = LatticeNetwork.extractPulseFromGenerators(level, pos, RADIUS, PUSH_PER_CYCLE, true);
        int want = Math.min(PUSH_PER_CYCLE, available);
        int taken = LatticeNetwork.extractPulseFromGenerators(level, pos, RADIUS, want, false);
        be.lastPulsePushed = LatticeNetwork.pushPulsePreferringAssist(level, network, benches, taken);
        if (be.lastPulsePushed < taken) {
            // Refund unused extract into the first nearby generator by re-inserting via network leftover —
            // leftover stays unspent; prefer reinserting into Ley/Drumheart near conductor.
            refundPulse(level, pos, taken - be.lastPulsePushed);
        }

        if (be.tickCounter % ITEM_INTERVAL == 0 && hasRoutingPower(network, be.lastPulsePushed)) {
            List<AncestralCacheBlockEntity> caches = LatticeNetwork.findCachesNearHubs(level, hubs, RADIUS);
            be.lastItemRouted = LatticeNetwork.routeEchoItems(level, benches, caches);
        } else {
            be.lastItemRouted = false;
        }
        be.setChanged();
    }

    private static void refundPulse(Level level, BlockPos origin, int amount) {
        if (amount <= 0) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int remaining = amount;
        for (int dx = -RADIUS; dx <= RADIUS && remaining > 0; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS && remaining > 0; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS && remaining > 0; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    var be = level.hasChunkAt(cursor) ? level.getBlockEntity(cursor) : null;
                    if (be instanceof DrumheartBlockEntity drum) {
                        remaining -= drum.insertPulse(remaining, false);
                    } else if (be instanceof LeyCollectorBlockEntity ley) {
                        remaining -= ley.insertPulse(remaining, false);
                    } else if (be instanceof PulseResonatorBlockEntity resonator) {
                        remaining -= resonator.insertPulse(remaining, false);
                    }
                }
            }
        }
    }

    private static boolean hasRoutingPower(List<ResonanceTotemBlockEntity> network, int pushed) {
        return pushed > 0 || network.stream().anyMatch(totem -> totem.getPulseStored() > 0);
    }

    /**
     * Manual strike: report network and burst-transfer Pulse into linked totem buffers.
     */
    public Component conductOnce() {
        if (level != null && level.hasNeighborSignal(worldPosition)) return Component.translatable("message.tribalpower.redstone.locked");
        if (level == null) {
            return Component.translatable("message.tribalpower.conductor.no_network");
        }
        List<ResonanceTotemBlockEntity> network = LatticeNetwork.collectChalkNetworkNear(level, worldPosition, RADIUS);
        networkSize = network.size();
        if (!LatticeNetwork.isConductable(network)) {
            assistActive = false;
            lastPulsePushed = 0;
            setChanged();
            return Component.translatable("message.tribalpower.conductor.no_network");
        }

        List<BlockPos> hubs = LatticeNetwork.networkHubs(network);
        List<SongBenchBlockEntity> benches = LatticeNetwork.findSongBenchesNearHubs(level, hubs, RADIUS);
        assistActive = false;
        for (SongBenchBlockEntity bench : benches) {
            if (bench.wantsPulseAssist()) {
                assistActive = true;
                break;
            }
        }

        int available = LatticeNetwork.extractPulseFromGenerators(level, worldPosition, RADIUS, CLICK_PUSH, true);
        int want = Math.min(CLICK_PUSH, available);
        int taken = LatticeNetwork.extractPulseFromGenerators(level, worldPosition, RADIUS, want, false);
        lastPulsePushed = LatticeNetwork.pushPulsePreferringAssist(level, network, benches, taken);
        if (lastPulsePushed < taken) {
            refundPulse(level, worldPosition, taken - lastPulsePushed);
        }

        if (hasRoutingPower(network, lastPulsePushed)) {
            List<AncestralCacheBlockEntity> caches = LatticeNetwork.findCachesNearHubs(level, hubs, RADIUS);
            lastItemRouted = LatticeNetwork.routeEchoItems(level, benches, caches);
        } else {
            lastItemRouted = false;
        }
        setChanged();

        if (assistActive && lastItemRouted) {
            return Component.translatable(
                    "message.tribalpower.conductor.assist_moved",
                    networkSize,
                    lastPulsePushed
            );
        }
        if (assistActive) {
            return Component.translatable(
                    "message.tribalpower.conductor.assist",
                    networkSize,
                    lastPulsePushed
            );
        }
        return Component.translatable(
                "message.tribalpower.conductor.pulse",
                networkSize,
                lastPulsePushed
        );
    }

    public int getNetworkSize() {
        return networkSize;
    }

    public int getLastPulsePushed() {
        return lastPulsePushed;
    }

    public boolean isAssistActive() {
        return assistActive;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TickCounter", tickCounter);
        tag.putInt("NetworkSize", networkSize);
        tag.putInt("LastPulsePushed", lastPulsePushed);
        tag.putBoolean("AssistActive", assistActive);
        tag.putBoolean("LastItemRouted", lastItemRouted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tickCounter = tag.getInt("TickCounter");
        networkSize = tag.getInt("NetworkSize");
        lastPulsePushed = tag.getInt("LastPulsePushed");
        assistActive = tag.getBoolean("AssistActive");
        lastItemRouted = tag.getBoolean("LastItemRouted");
    }
}
