package tk.darrow.tribalpower.guardian;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.SpiritEffects;

/**
 * A guardian's altar. It knows which guardian it belongs to (set by its structure), and it calls that guardian
 * when a player lays down enough of the biome's reagent: altar or nothing (a cap of polished deepslate below and
 * a candle at each corner), one guardian at a time, a rest between calls, and room above it to rise into.
 */
public class GuardianAltarBlockEntity extends BlockEntity {
    private Guardian guardian = Guardian.SLAG_TITAN;
    private UUID boss;
    private long lastWake = Long.MIN_VALUE;

    public GuardianAltarBlockEntity(BlockPos pos, BlockState state) {
        super(GuardianRegistry.ALTAR_ENTITY.get(), pos, state);
    }

    public Guardian guardian() { return guardian; }
    public void setGuardian(Guardian guardian) { this.guardian = guardian; setChanged(); }

    /** The guardian this altar called, if it still stands. */
    public GuardianEntity living(ServerLevel server) {
        if (boss == null) return null;
        return server.getEntity(boss) instanceof GuardianEntity entity && entity.isAlive() ? entity : null;
    }

    public void onGuardianGone() { boss = null; setChanged(); }

    /** Whether the altar stands on its cap with its four candles: without them it is a stone. */
    public boolean onAltar(ServerLevel server) {
        if (!server.getBlockState(worldPosition.below()).is(Blocks.POLISHED_DEEPSLATE)) return false;
        for (int dx : new int[] {-1, 1})
            for (int dz : new int[] {-1, 1})
                if (!(server.getBlockState(worldPosition.offset(dx, 0, dz)).getBlock() instanceof AbstractCandleBlock)) return false;
        return true;
    }

    /** Whether there is room above the altar for the guardian to rise into (a 3.0 lesson: no boss wakes into a ceiling). */
    public static boolean headroom(ServerLevel server, BlockPos pos, Guardian guardian) {
        int reach = (int) Math.ceil(guardian.width / 2) ;
        int height = (int) Math.ceil(guardian.height) + 1;
        for (int y = 1; y <= height; y++)
            for (int dx = -reach; dx <= reach; dx++)
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos at = pos.offset(dx, y, dz);
                    BlockState state = server.getBlockState(at);
                    if (!state.getCollisionShape(server, at).isEmpty() && !(state.getBlock() instanceof AbstractCandleBlock)) return false;
                }
        return true;
    }

    /** A player lays the call on the altar. Returns null when the guardian rises, or why it did not. */
    public Component call(ServerLevel server, Player player, ItemStack offered) {
        if (!offered.is(guardian.callItem()))
            return Component.translatable("message.tribalpower.guardian_altar.wants", Component.translatable(guardian.nameKey()),
                    TribalConfig.guardianCallCost(), guardian.callItem().getDescription());
        if (living(server) != null) return Component.translatable("message.tribalpower.guardian_altar.awake", Component.translatable(guardian.nameKey()));
        if (!onAltar(server)) return Component.translatable("message.tribalpower.guardian_altar.no_altar");
        long cooldown = TribalConfig.guardianCooldownMinutes() * 1200L;
        long since = server.getGameTime() - lastWake;
        if (lastWake != Long.MIN_VALUE && since < cooldown)
            return Component.translatable("message.tribalpower.guardian_altar.resting", (cooldown - since) / 1200L + 1);
        if (offered.getCount() < TribalConfig.guardianCallCost())
            return Component.translatable("message.tribalpower.guardian_altar.wants", Component.translatable(guardian.nameKey()),
                    TribalConfig.guardianCallCost(), guardian.callItem().getDescription());
        if (!headroom(server, worldPosition, guardian)) return Component.translatable("message.tribalpower.guardian_altar.no_room");
        GuardianEntity entity = GuardianRegistry.ENTITIES.get(guardian).get().create(server);
        if (entity == null) return Component.translatable("message.tribalpower.guardian_altar.no_altar");
        if (!player.getAbilities().instabuild) offered.shrink(TribalConfig.guardianCallCost());
        entity.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, server.random.nextFloat() * 360F, 0);
        entity.bindAltar(worldPosition);
        entity.finalizeSpawn(server, server.getCurrentDifficultyAt(worldPosition), MobSpawnType.EVENT, null);
        server.addFreshEntity(entity);
        boss = entity.getUUID();
        lastWake = server.getGameTime();
        server.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.6F);
        SpiritEffects.ring(server, worldPosition.getCenter().add(0, 1, 0), guardian.tribe.attunement(), 4, 24);
        server.sendParticles(ParticleTypes.SOUL, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 40, 1.2, 1.0, 1.2, 0.03);
        for (Player near : server.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(GuardianEntity.RESET_RANGE)))
            near.displayClientMessage(Component.translatable("message.tribalpower.guardian." + guardian.id + ".rises"), false);
        setChanged();
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Guardian", guardian.id);
        if (boss != null) tag.putUUID("Boss", boss);
        if (lastWake != Long.MIN_VALUE) tag.putLong("LastWake", lastWake);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Guardian named = Guardian.byId(tag.getString("Guardian"));
        if (named != null) guardian = named;
        boss = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
        lastWake = tag.contains("LastWake") ? tag.getLong("LastWake") : Long.MIN_VALUE;
    }
}
