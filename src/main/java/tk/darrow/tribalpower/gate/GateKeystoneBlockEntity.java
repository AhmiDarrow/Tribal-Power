package tk.darrow.tribalpower.gate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.Diagnosable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseStorage;
import tk.darrow.tribalpower.camp.Ownership;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.pattern.ModPatterns;
import tk.darrow.tribalpower.pattern.PatternMatcher;
import tk.darrow.tribalpower.pattern.RitualPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The stone that remembers where the other end is (design 3.1 section 8).
 *
 * <p>A keystone is the whole gate as far as the rest of the mod is concerned: it holds the Pulse, it owns
 * the record in the book, it lights and extinguishes the plane, and it is what a comparator reads.
 *
 * <p>Design note: the design gives the Way Gate a 400 Pulse lighting cost out of a 600 store but leaves
 * the Far Gate's unstated. Taken here at the same two-thirds of the store, so 1,200 out of 2,000.
 */
public class GateKeystoneBlockEntity extends BlockEntity implements PulseHandler, Diagnosable, Ownership.Owned {
    /** Which gate a keystone turned out to be, decided by the frame built around it. */
    public enum Kind {
        WAY(600, 400, ModPatterns.WAY_GATE),
        FAR(2000, 1200, ModPatterns.FAR_GATE);

        private final int capacity;
        private final int lightCost;
        private final RitualPattern pattern;

        Kind(int capacity, int lightCost, RitualPattern pattern) {
            this.capacity = capacity;
            this.lightCost = lightCost;
            this.pattern = pattern;
        }

        public int capacity() { return capacity; }
        public int lightCost() { return lightCost; }
        public RitualPattern pattern() { return pattern; }
        public int travelCost() { return this == FAR ? TribalConfig.farGateTravelCost() : TribalConfig.gateTravelCost(); }
        /** A Far Gate crosses dimensions, so it holds each traveller off for a moment afterwards. */
        public int cooldownTicks() { return this == FAR ? 40 : 0; }
        public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** How long the comparator reports a transit for, so a two-tick pulse is readable by a repeater. */
    public static final int TRANSIT_SIGNAL_TICKS = 2;
    public static final int CELL_CHARGE = 100;
    public static final int LINK_RANGE = 64;

    private final PulseStorage pulse = new PulseStorage(Kind.FAR.capacity());
    private UUID gateId;
    private UUID owner;
    private boolean lit;
    private long transitUntil = Long.MIN_VALUE;
    private boolean lastSignal;
    /** The partner dimension's colour, computed on the server and synced so the plane shows where it goes. */
    private int destinationTint = GateTint.UNKNOWN;

    private final tk.darrow.tribalpower.pattern.PatternState wayPattern =
            new tk.darrow.tribalpower.pattern.PatternState(ModPatterns.WAY_GATE);
    private final tk.darrow.tribalpower.pattern.PatternState farPattern =
            new tk.darrow.tribalpower.pattern.PatternState(ModPatterns.FAR_GATE);

    public GateKeystoneBlockEntity(BlockPos pos, BlockState state) {
        super(GateRegistry.KEYSTONE_TYPE.get(), pos, state);
    }

    // ---- shape -----------------------------------------------------------------------------

    /** The kind of gate the frame around this keystone makes it. Far is checked first: it is the taller. */
    @Nullable
    public Kind kind(Level level) {
        if (farPattern.get(level, worldPosition).found()) return Kind.FAR;
        if (wayPattern.get(level, worldPosition).found()) return Kind.WAY;
        return null;
    }

    public PatternMatcher.Match match(Level level) {
        PatternMatcher.Match far = farPattern.get(level, worldPosition);
        if (far.found()) return far;
        PatternMatcher.Match way = wayPattern.get(level, worldPosition);
        // Report whichever shape the builder got closest to, so the miss list is about the gate they meant.
        return way.found() || way.misses().size() <= far.misses().size() ? way : far;
    }

    public void onNeighbourChanged(BlockPos changed) {
        wayPattern.onNeighbourChanged(worldPosition, changed);
        farPattern.onNeighbourChanged(worldPosition, changed);
    }

    /** A Far Gate needs the Loom voice nearby, which is what puts other dimensions behind The Unsung. */
    public boolean loomPresent(Level level) {
        return LatticeNetwork.hasAttunement(level, worldPosition, LatticeNetwork.DEFAULT_RADIUS, Attunement.LOOM);
    }

    // ---- the book --------------------------------------------------------------------------

    @Override public UUID owner() { return owner; }
    @Override public void setOwner(UUID owner) { this.owner = owner; setChanged(); }

    public UUID gateId() { return gateId; }

    /** This keystone's record, created on first ask. */
    public GateSavedData.Gate record(ServerLevel level) {
        GateSavedData data = GateSavedData.get(level.getServer());
        GateSavedData.Gate gate = gateId == null ? null : data.gate(gateId);
        if (gate == null) {
            gate = data.record(level, worldPosition, defaultName(), owner);
            gateId = gate.id();
            setChanged();
        }
        return gate;
    }

    private String defaultName() {
        return "Gate " + worldPosition.getX() + " " + worldPosition.getY() + " " + worldPosition.getZ();
    }

    @Nullable
    public GateSavedData.Gate partner(ServerLevel level) {
        GateSavedData.Gate self = record(level);
        return self.partner() == null ? null : GateSavedData.get(level.getServer()).gate(self.partner());
    }

    public int destinationTint() { return destinationTint; }

    /** Recomputes the synced tint. Cheap, and only ever called when something about the link changed. */
    public void refreshTint(ServerLevel level) {
        GateSavedData.Gate partner = partner(level);
        int tint = partner == null ? GateTint.UNKNOWN : GateTint.of(partner.dimension());
        if (tint == destinationTint) return;
        destinationTint = tint;
        changed();
    }

    // ---- lighting --------------------------------------------------------------------------

    public boolean lit() { return lit; }

    /** Redstone held high takes the plane down and keeps it down. */
    public boolean stilled() { return level != null && level.hasNeighborSignal(worldPosition); }

    /**
     * Lights the plane, spending the kind's lighting cost out of this keystone's own store.
     * @return null on success, otherwise why nothing happened
     */
    @Nullable
    public Component light(ServerLevel level) {
        if (lit) return null;
        Kind kind = kind(level);
        if (kind == null) return Component.translatable("message.tribalpower.gate.no_frame");
        if (kind == Kind.FAR && !TribalConfig.farGatesEnabled())
            return Component.translatable("message.tribalpower.gate.far_disabled");
        if (kind == Kind.FAR && !loomPresent(level))
            return Component.translatable("message.tribalpower.gate.no_loom");
        // A struck signal is high at the moment it is struck, so lighting cannot refuse on that.
        // A signal that is still high a second later is a held one, and the beat takes the plane
        // back down; transit is refused while stilled either way.
        if (pulse.getPulseStored() < kind.lightCost())
            return Component.translatable("message.tribalpower.gate.need_pulse", kind.lightCost(), pulse.getPulseStored());
        pulse.extractPulse(kind.lightCost(), false);
        lit = true;
        record(level);
        refreshTint(level);
        GatePortal.fill(level, this, kind);
        tk.darrow.tribalpower.sound.ModSounds.play(level, worldPosition,
                tk.darrow.tribalpower.sound.ModSounds.GATE_HUM, 0.65F, 0.9F);
        tk.darrow.tribalpower.camp.CampHooks.award(level, owner, "journey/circle");
        tk.darrow.tribalpower.camp.CampHooks.award(level, owner, "journey/open_the_way");
        changed();
        return null;
    }

    public void extinguish(ServerLevel level) {
        if (!lit) return;
        lit = false;
        GatePortal.clear(level, this);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.9F);
        changed();
    }

    /**
     * A struck signal calls once: light a dark gate, darken a lit one. Only a rising edge counts.
     *
     * <p>Acting on the level rather than the edge would make this re-entrant: lighting a gate places the
     * plane, and those blocks are neighbours of the keystone, so the toggle would arrive back here and
     * switch the gate straight off again. Holding the line high is handled where it belongs -- the beat
     * takes a stilled plane down, and transit refuses while stilled.
     */
    public void onRedstoneChanged(ServerLevel level) {
        boolean signal = level.hasNeighborSignal(worldPosition);
        boolean rising = signal && !lastSignal;
        if (signal == lastSignal) return;
        lastSignal = signal;
        setChanged();
        if (!rising) return;
        if (lit) extinguish(level);
        else light(level);
    }

    // ---- travel ----------------------------------------------------------------------------

    public void markTransit() {
        if (level == null) return;
        transitUntil = level.getGameTime() + TRANSIT_SIGNAL_TICKS;
        changed();
        // The pulse has to fall on its own. The beat only runs once a second and returns early when the
        // gate is dark, so without this the comparator would sit at 15 until something else nudged it.
        if (level instanceof ServerLevel server)
            server.scheduleTick(worldPosition, getBlockState().getBlock(), TRANSIT_SIGNAL_TICKS + 1);
    }

    /** The scheduled tick at the end of a transit pulse: tell the comparator the reading has changed. */
    public void onTransitEnded() {
        changed();
    }

    /**
     * A struck signal is an edge, so a keystone placed into an already-powered spot must start from what
     * is actually there -- otherwise the first unrelated neighbour update reads as a rise and spends 400
     * Pulse lighting a gate nobody struck.
     */
    public void seedSignal(Level level) {
        lastSignal = level.hasNeighborSignal(worldPosition);
        setChanged();
    }

    public boolean inTransit() { return level != null && level.getGameTime() < transitUntil; }

    /** 0 unlinked, 1-14 stored Pulse, 15 mid-transit (design 3.1 section 2). */
    public int signal(Level level) {
        if (inTransit()) return 15;
        if (!(level instanceof ServerLevel server)) return 0;
        GateSavedData.Gate gate = gateId == null ? null : GateSavedData.get(server.getServer()).gate(gateId);
        if (gate == null || gate.partner() == null) return 0;
        int stored = pulse.getPulseStored();
        return stored == 0 ? 1 : Math.min(14, 1 + 13 * stored / Math.max(1, capacityFor(level)));
    }

    private int capacityFor(Level level) {
        Kind kind = kind(level);
        return kind == null ? Kind.WAY.capacity() : kind.capacity();
    }

    // ---- pulse -----------------------------------------------------------------------------

    @Override public int getPulseStored() { return pulse.getPulseStored(); }

    @Override
    public int getPulseCapacity() {
        return level == null ? Kind.WAY.capacity() : capacityFor(level);
    }

    @Override
    public int insertPulse(int amount, boolean simulate) {
        int room = getPulseCapacity() - pulse.getPulseStored();
        int accepted = pulse.insertPulse(Math.min(amount, Math.max(0, room)), simulate);
        if (!simulate && accepted > 0) changed();
        return accepted;
    }

    @Override
    public int extractPulse(int amount, boolean simulate) {
        // A gate's store is for the gate. Draining it from the lattice would strand travellers mid-thread.
        return 0;
    }

    /** Spends travel Pulse, refunding nothing because nothing was taken when it fails. */
    public boolean spendTravel(Kind kind) {
        if (pulse.getPulseStored() < kind.travelCost()) return false;
        pulse.extractPulse(kind.travelCost(), false);
        changed();
        return true;
    }

    public void refund(Kind kind) {
        pulse.insertPulse(kind.travelCost(), false);
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GateKeystoneBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        be.refreshTint(server);
        if (!be.lit) return;
        if (be.stilled() || be.kind(level) == null) { be.extinguish(server); return; }
        if ((level.getGameTime() + pos.asLong()) % 40 == 0) {
            tk.darrow.tribalpower.sound.ModSounds.play(level, pos,
                    tk.darrow.tribalpower.sound.ModSounds.GATE_HUM, 0.18F, 0.85F);
        }
        // Draw from the lattice so a gate on a powered base tops itself back up between trips.
        int room = be.getPulseCapacity() - be.pulse.getPulseStored();
        if (room > 0) be.pulse.insertPulse(
                LatticeNetwork.extractPulseNearby(level, pos, LatticeNetwork.DEFAULT_RADIUS, Math.min(room, 40), false), false);
        GatePortal.fill(server, be, be.kind(level));
    }

    // ---- diagnostics -----------------------------------------------------------------------

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        Kind kind = kind(server);
        if (kind == null) {
            lines.addAll(match(server).report(3));
            return lines;
        }
        lines.add(Component.translatable("diag.tribalpower.gate.kind",
                Component.translatable("message.tribalpower.gate.kind." + kind.key()),
                kind.lightCost(), kind.travelCost()));
        GateSavedData.Gate self = record(server);
        lines.add(Component.translatable("diag.tribalpower.gate.name", self.name()));
        GateSavedData.Gate partner = partner(server);
        if (partner == null)
            lines.add(Component.translatable("diag.tribalpower.gate.unlinked").withStyle(ChatFormatting.YELLOW));
        else
            lines.add(Component.translatable("diag.tribalpower.gate.partner", partner.name(),
                    partner.dimension(), partner.pos().getX(), partner.pos().getY(), partner.pos().getZ()));
        lines.add(Component.translatable(lit ? "diag.tribalpower.gate.lit" : "diag.tribalpower.gate.dark"));
        if (kind == Kind.FAR && !loomPresent(server))
            lines.add(Component.translatable("message.tribalpower.gate.no_loom").withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    // ---- persistence -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pulse.save(tag);
        tag.putBoolean("Lit", lit);
        tag.putBoolean("LastSignal", lastSignal);
        tag.putInt("Tint", destinationTint);
        if (gateId != null) tag.putUUID("GateId", gateId);
        Ownership.save(tag, owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse.load(tag);
        lit = tag.getBoolean("Lit");
        lastSignal = tag.getBoolean("LastSignal");
        destinationTint = tag.contains("Tint") ? tag.getInt("Tint") : GateTint.UNKNOWN;
        gateId = tag.hasUUID("GateId") ? tag.getUUID("GateId") : null;
        owner = Ownership.load(tag);
        wayPattern.invalidate();
        farPattern.invalidate();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Lit", lit);
        tag.putInt("Tint", destinationTint);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
