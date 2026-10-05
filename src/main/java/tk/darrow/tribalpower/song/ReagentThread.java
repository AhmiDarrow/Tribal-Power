package tk.darrow.tribalpower.song;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.MarchThreat;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.familiar.FamiliarData;
import tk.darrow.tribalpower.leyheart.LeyHeartBlockEntity;

/**
 * A reagent's Thread: how strong a line the creature it came off was. Thread I to III; a plain reagent has none.
 *
 * <p>It is read off the creature when the reagent leaves it, by a kill or a brush: its strongest bloodline thread
 * ({@link FamiliarData#phenotype}) past the wild ceiling's 2, plus one for a March elite. Wild stock tops out at
 * 3, so a wild creature sheds Thread I at best and an elite Thread II; a bred line reaches II at thread 4, and
 * only an Exalted line (thread 5) sheds III.
 *
 * <p>The Thread lives on the stack as the {@code tribalpower:reagent_thread} component, so reagents of different
 * Threads never stack, and the Reagent Pouch leaves a threaded reagent in your bags rather than flatten it into a
 * plain count. Everything else that takes a reagent (songs, remedies, recipes, feeding) takes it as any other;
 * the Ley Heart is what burns a stronger Thread hotter.
 */
public final class ReagentThread {
    public static final int MAX = 3;

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TribalPower.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> THREAD =
            COMPONENTS.register("reagent_thread", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(1, MAX))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ReagentThread() {}

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    /** The stack's Thread, 0 for a plain reagent (or anything that is not one). */
    public static int get(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Integer thread = stack.get(THREAD.get());
        return thread == null ? 0 : Math.clamp(thread, 0, MAX);
    }

    /** Marks {@code stack} with a Thread, or clears it for 0. Returns the same stack. */
    public static ItemStack with(ItemStack stack, int thread) {
        int clamped = Math.clamp(thread, 0, MAX);
        if (clamped <= 0) stack.remove(THREAD.get());
        else stack.set(THREAD.get(), clamped);
        return stack;
    }

    /** The Thread a reagent leaving this creature carries. 0 for anything that is not a lattice creature. */
    public static int of(LivingEntity creature) {
        if (!(creature instanceof Familiar familiar)) return 0;
        int best = 0;
        FamiliarData lattice = familiar.lattice();
        if (lattice.rolled())
            for (FamiliarData.Thread thread : FamiliarData.Thread.values()) best = Math.max(best, lattice.phenotype(thread));
        int thread = Math.max(0, best - 2) + (elite(creature) ? 1 : 0);
        return Math.min(MAX, thread);
    }

    /** {@code stack}, marked with the Thread of the creature it came off. For brushing. */
    public static ItemStack shed(LivingEntity creature, ItemStack stack) {
        return with(stack, of(creature));
    }

    public static boolean elite(LivingEntity creature) {
        var health = creature.getAttribute(Attributes.MAX_HEALTH);
        return health != null && health.hasModifier(MarchThreat.ELITE);
    }

    /** A creature's reagents fall carrying its Thread. Loot tables drop plain stacks; this marks them. */
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity creature = event.getEntity();
        if (creature.level().isClientSide || !(creature instanceof Familiar)) return;
        int thread = of(creature);
        if (thread <= 0) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!Reagents.isReagent(stack) || get(stack) > 0) continue;
            drop.setItem(with(stack.copy(), thread));
        }
    }

    /** "Thread II", and what it is worth in a Ley Heart. */
    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int thread = get(stack);
        if (thread <= 0 || !Reagents.isReagent(stack)) return;
        var lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        lines.add(at, Component.translatable("item.tribalpower.reagent.thread.heart",
                LeyHeartBlockEntity.reagentQuarters(thread), LeyHeartBlockEntity.reagentTicks(thread) / 20).withStyle(ChatFormatting.GRAY));
        lines.add(at, name(thread).withStyle(ChatFormatting.GOLD));
    }

    /** "Thread I", "Thread II", "Thread III", or "Plain" for 0. */
    public static net.minecraft.network.chat.MutableComponent name(int thread) {
        return Component.translatable("reagent.tribalpower.thread." + Math.clamp(thread, 0, MAX));
    }
}
