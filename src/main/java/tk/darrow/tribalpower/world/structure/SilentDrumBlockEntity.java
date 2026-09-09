package tk.darrow.tribalpower.world.structure;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.boss.TheUnsungEntity;
import tk.darrow.tribalpower.effect.SpiritEffects;

/**
 * Records strike timestamps. Four beats spaced {@link #MIN_GAP}–{@link #MAX_GAP} ticks apart wake The Unsung
 * (once per {@link #COOLDOWN_MILLIS} of real time; a second wake while one is alive is ignored) or, while The
 * Unsung is in its Silence phase, resync it so it can be struck.
 */
public class SilentDrumBlockEntity extends BlockEntity {
    public static final int BEATS_NEEDED = 4;
    public static final int MIN_GAP = 16;
    public static final int MAX_GAP = 28;
    public static final int DEBOUNCE = 4;
    public static final long COOLDOWN_MILLIS = 20L * 60L * 1000L;
    public static final double BOSS_RANGE = 48;

    private final long[] beats = new long[BEATS_NEEDED];
    private int recorded;
    private long lastWakeMillis = Long.MIN_VALUE / 2;
    private UUID boss;
    private int glow;

    public SilentDrumBlockEntity(BlockPos pos, BlockState state) { super(MarchRegistry.SILENT_DRUM_BE.get(), pos, state); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SilentDrumBlockEntity drum) {
        if (drum.glow > 0) drum.glow--;
        // a half-finished rhythm decays once the window has passed
        if (drum.recorded > 0 && level.getGameTime() - drum.beats[drum.recorded - 1] > MAX_GAP + 4) {
            drum.recorded = 0;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** Strike the drum. Returns the number of good beats recorded so far (0 after a reset or a completed rhythm). */
    public int strike(Player player) {
        if (!(level instanceof ServerLevel server)) return recorded;
        long now = server.getGameTime();
        if (recorded > 0 && now - beats[recorded - 1] < DEBOUNCE) return recorded;
        boolean inTime = recorded == 0 || (now - beats[recorded - 1] >= MIN_GAP && now - beats[recorded - 1] <= MAX_GAP);
        if (!inTime) {
            recorded = 0;
            server.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.9F, 0.55F);
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 6, 0.25, 0.05, 0.25, 0.01);
            if (player != null) player.displayClientMessage(Component.translatable("message.tribalpower.silent_drum.reset"), true);
            server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            return 0;
        }
        beats[recorded++] = now;
        glow = 12;
        server.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 1.4F, 0.5F + 0.05F * recorded);
        SpiritEffects.ring(server, worldPosition.getCenter().add(0, 0.6, 0), Attunement.SPIRIT, 0.6 + 0.35 * recorded, 8 + 4 * recorded);
        if (recorded >= BEATS_NEEDED) {
            recorded = 0;
            complete(server, player);
        }
        server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        setChanged();
        return recorded;
    }

    private void complete(ServerLevel server, Player player) {
        TheUnsungEntity alive = livingBoss(server);
        if (alive != null) {
            boolean resynced = alive.resync();
            if (player != null) player.displayClientMessage(Component.translatable(resynced ? "message.tribalpower.silent_drum.resync" : "message.tribalpower.silent_drum.already_awake"), true);
            server.playSound(null, worldPosition, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.2F, resynced ? 0.6F : 1.4F);
            return;
        }
        long real = System.currentTimeMillis();
        if (real - lastWakeMillis < COOLDOWN_MILLIS) {
            if (player != null) player.displayClientMessage(Component.translatable("message.tribalpower.silent_drum.resting",
                    (COOLDOWN_MILLIS - (real - lastWakeMillis)) / 60000L + 1), true);
            server.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.9F, 0.5F);
            return;
        }
        TheUnsungEntity unsung = MarchRegistry.THE_UNSUNG.get().create(server);
        if (unsung == null) return;
        unsung.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, server.random.nextFloat() * 360F, 0);
        unsung.bindDrum(worldPosition);
        unsung.finalizeSpawn(server, server.getCurrentDifficultyAt(worldPosition), MobSpawnType.EVENT, null);
        server.addFreshEntity(unsung);
        boss = unsung.getUUID();
        lastWakeMillis = real;
        server.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.55F);
        SpiritEffects.ring(server, worldPosition.getCenter().add(0, 1, 0), Attunement.LOOM, 4, 24);
        server.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 40, 1.2, 1.0, 1.2, 0.03);
        for (Player near : server.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(BOSS_RANGE)))
            near.displayClientMessage(Component.translatable("message.tribalpower.silent_drum.wake"), false);
        setChanged();
    }

    /** The Unsung this drum woke, if still alive and within range. */
    public TheUnsungEntity livingBoss(ServerLevel server) {
        if (boss != null && server.getEntity(boss) instanceof TheUnsungEntity unsung && unsung.isAlive()) return unsung;
        for (TheUnsungEntity unsung : server.getEntitiesOfClass(TheUnsungEntity.class, new AABB(worldPosition).inflate(BOSS_RANGE)))
            if (unsung.isAlive()) { boss = unsung.getUUID(); return unsung; }
        boss = null;
        return null;
    }

    /** Called by The Unsung when it resets because no player stayed near: the drum may be struck again at once. */
    public void onBossReset() { boss = null; lastWakeMillis = Long.MIN_VALUE / 2; setChanged(); }

    public int recordedBeats() { return recorded; }
    public boolean resting() { return System.currentTimeMillis() - lastWakeMillis < COOLDOWN_MILLIS; }
    public int signal() { return recorded == 0 ? (glow > 0 ? 15 : 0) : recorded * 3; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LastWake", lastWakeMillis);
        if (boss != null) tag.putUUID("Boss", boss);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastWakeMillis = tag.contains("LastWake") ? tag.getLong("LastWake") : Long.MIN_VALUE / 2;
        boss = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
    }
}
