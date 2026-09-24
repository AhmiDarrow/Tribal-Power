package tk.darrow.tribalpower.healing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.familiar.Familiar;

/**
 * The Healing Circle, a Spirit rite. Cast, it heals and cleanses everyone inside -- players and bonded familiars
 * -- blesses them, and calls back any familiar whose Spirit Remnant was laid on one of its pedestals. Then the
 * circle stays open for the rite's duration, mending whoever stands in it every two seconds.
 */
public final class HealingCircles extends SavedData {
    private static final int[][] PEDESTALS = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};

    record Circle(String dimension, BlockPos centre, long expiry) {}

    private final List<Circle> circles = new ArrayList<>();

    private static HealingCircles get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(HealingCircles::new, HealingCircles::load), "tribalpower_healing_circles");
    }

    /** Casts the circle at a brazier and keeps it open for {@code duration} ticks. Returns how many were called back. */
    public static int cast(ServerLevel level, BlockPos brazier, int duration) {
        for (LivingEntity body : inside(level, brazier)) {
            cleanse(body);
            body.heal((float) TribalConfig.circleCastHeal());
            int blessing = TribalConfig.circleBlessingMinutes() * 60 * 20;
            if (blessing > 0) body.addEffect(new MobEffectInstance(HealingRegistry.SPIRIT_BLESSING, blessing, 0));
        }
        int revived = TribalConfig.circleRevives() ? revive(level, brazier) : 0;
        if (duration > 0) {
            HealingCircles data = get(level);
            data.circles.add(new Circle(level.dimension().location().toString(), brazier.immutable(), level.getGameTime() + duration));
            data.setDirty();
        }
        return revived;
    }

    /** Remnants on the circle's four pedestals come back, whole, and the pedestal is emptied. */
    private static int revive(ServerLevel level, BlockPos brazier) {
        int count = 0;
        for (int[] offset : PEDESTALS) {
            BlockPos at = brazier.offset(offset[0], 0, offset[1]);
            if (!(level.getBlockEntity(at) instanceof RitePedestalBlockEntity pedestal)) continue;
            ItemStack held = pedestal.held();
            if (!(held.getItem() instanceof SpiritRemnantItem)) continue;
            if (SpiritRemnantItem.revive(level, at, held) == null) continue;
            pedestal.removeItem(RitePedestalBlockEntity.SLOT, 1);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.getX() + 0.5, at.getY() + 1.4, at.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.2);
            count++;
        }
        return count;
    }

    /** Players and bonded familiars within the circle's reach. */
    public static List<LivingEntity> inside(ServerLevel level, BlockPos centre) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(centre).inflate(TribalConfig.circleRadius()),
                e -> e.isAlive() && !e.isSpectator() && (e instanceof Player || e instanceof Familiar familiar && familiar.isBonded()));
    }

    private static void cleanse(LivingEntity body) {
        for (var effect : List.copyOf(body.getActiveEffects()))
            if (!effect.getEffect().value().isBeneficial()) body.removeEffect(effect.getEffect());
    }

    /** Every two seconds, every open circle in this level mends whoever is inside. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 40 != 0) return;
        HealingCircles data = get(level);
        if (data.circles.isEmpty()) return;
        String dim = level.dimension().location().toString();
        long now = level.getGameTime();
        boolean changed = data.circles.removeIf(circle -> circle.dimension.equals(dim) && circle.expiry <= now);
        for (Circle circle : data.circles) {
            if (!circle.dimension.equals(dim) || !level.isLoaded(circle.centre)) continue;
            for (LivingEntity body : inside(level, circle.centre)) {
                body.heal((float) TribalConfig.circleBeatHeal());
                body.removeEffect(HealingRegistry.SPIRIT_SICKNESS);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, circle.centre.getX() + 0.5, circle.centre.getY() + 1, circle.centre.getZ() + 0.5,
                    12, TribalConfig.circleRadius() * 0.4, 0.4, TribalConfig.circleRadius() * 0.4, 0);
        }
        if (changed) data.setDirty();
    }

    /** How many circles are open in this level, for tests and diagnosis. */
    public static int open(ServerLevel level) {
        String dim = level.dimension().location().toString();
        return (int) get(level).circles.stream().filter(c -> c.dimension.equals(dim) && c.expiry > level.getGameTime()).count();
    }

    public static HealingCircles load(CompoundTag tag, HolderLookup.Provider registries) {
        HealingCircles data = new HealingCircles();
        ListTag list = tag.getList("Circles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.circles.add(new Circle(entry.getString("Dim"), BlockPos.of(entry.getLong("Centre")), entry.getLong("Expiry")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Circle circle : circles) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dim", circle.dimension);
            entry.putLong("Centre", circle.centre.asLong());
            entry.putLong("Expiry", circle.expiry);
            list.add(entry);
        }
        tag.put("Circles", list);
        return tag;
    }
}
