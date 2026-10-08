package tk.darrow.tribalpower.wildlife;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import tk.darrow.tribalpower.world.ModDimensions;

import java.util.ArrayList;
import java.util.List;

/**
 * A rod cast in the March pulls up the March's own fish. Every cod, salmon or tropical fish the vanilla table
 * rolls becomes a Raw Glimmerfin, or now and then a Raw Silt Eel; junk and treasure come up as they are.
 * An empty bucket in the other hand lands a glimmerfin alive, as a Bucket of Glimmerfin, so a pond can be
 * stocked from the bank without chasing the shoal.
 *
 * <p>The event's drop list is a copy, so the catch is cancelled and landed here the way the hook lands it.
 */
public final class MarchFishing {
    /** One fish catch in four is an eel; the rest are glimmerfin. */
    public static final int EEL_ONE_IN = 4;

    private MarchFishing() {}

    /** The March's catch for a vanilla haul, or null when nothing in it was a fish and vanilla should land it. */
    public static List<ItemStack> marchCatch(List<ItemStack> drops, RandomSource random, boolean bucketInHand) {
        List<ItemStack> out = new ArrayList<>(drops.size());
        boolean fished = false;
        for (ItemStack drop : drops) {
            if (!drop.is(ItemTags.FISHES)) { out.add(drop); continue; }
            fished = true;
            boolean eel = random.nextInt(EEL_ONE_IN) == 0;
            if (eel) out.add(new ItemStack(Wildlife.RAW_SILT_EEL.get(), drop.getCount()));
            else if (bucketInHand) out.add(new ItemStack(Wildlife.GLIMMERFIN_BUCKET.get()));
            else out.add(new ItemStack(Wildlife.RAW_GLIMMERFIN.get(), drop.getCount()));
        }
        return fished ? out : null;
    }

    public static void onFished(ItemFishedEvent event) {
        var hook = event.getHookEntity();
        if (!(hook.level() instanceof net.minecraft.server.level.ServerLevel level) || !level.dimension().equals(ModDimensions.THE_MARCH)) return;
        Player player = event.getEntity();
        if (player == null) return;
        ItemStack offhand = player.getOffhandItem();
        boolean bucket = offhand.is(Items.BUCKET);
        List<ItemStack> landed = marchCatch(event.getDrops(), level.random, bucket);
        if (landed == null) return;
        event.setCanceled(true);
        boolean bucketed = false;
        for (ItemStack stack : landed) {
            if (stack.is(Wildlife.GLIMMERFIN_BUCKET.get())) bucketed = true;
            ItemEntity item = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), stack);
            double dx = player.getX() - hook.getX(), dy = player.getY() - hook.getY(), dz = player.getZ() - hook.getZ();
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
            level.addFreshEntity(item);
            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, level.random.nextInt(6) + 1));
            if (player instanceof ServerPlayer server && (stack.is(ItemTags.FISHES) || stack.is(Wildlife.GLIMMERFIN_BUCKET.get())))
                server.awardStat(Stats.FISH_CAUGHT, 1);
        }
        if (bucketed && !player.getAbilities().instabuild) offhand.shrink(1);
    }
}
