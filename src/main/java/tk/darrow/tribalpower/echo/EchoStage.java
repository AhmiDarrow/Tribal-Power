package tk.darrow.tribalpower.echo;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.item.ModItems;

/**
 * Code-defined Echo refine table: Shatter → Attune → Bind → Manifest.
 * Live processing runs on the Song Bench (not a furnace, not the shrine marker blocks).
 * Each stage needs Spirit Pulse from nearby generators/totems and a matching Resonance Totem.
 */
public enum EchoStage {
    SHATTER(Attunement.EARTH, 40, 8),
    ATTUNE(Attunement.FIRE, 60, 10),
    BIND(Attunement.WATER, 80, 12),
    MANIFEST(Attunement.SPIRIT, 100, 16);

    private final Attunement required;
    private final int workTicks;
    private final int pulsePerTick;

    EchoStage(Attunement required, int workTicks, int pulsePerTick) {
        this.required = required;
        this.workTicks = workTicks;
        this.pulsePerTick = pulsePerTick;
    }

    public Attunement requiredAttunement() {
        return required;
    }

    public int workTicks() {
        return workTicks;
    }

    public int pulsePerTick() {
        return pulsePerTick;
    }

    @Nullable
    public static EchoStage forInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (isShatterFeed(item)) {
            return SHATTER;
        }
        if (stack.is(ModItems.ECHO_SHARD.get())) {
            return ATTUNE;
        }
        if (stack.is(ModItems.ATTUNED_ECHO.get())) {
            return BIND;
        }
        if (stack.is(ModItems.BOUND_ECHO.get())) {
            return MANIFEST;
        }
        return null;
    }

    public ItemLike output() {
        return switch (this) {
            case SHATTER -> ModItems.ECHO_SHARD.get();
            case ATTUNE -> ModItems.ATTUNED_ECHO.get();
            case BIND -> ModItems.BOUND_ECHO.get();
            case MANIFEST -> ModItems.MANIFESTED_INGOT.get();
        };
    }

    /**
     * Shatter accepts raw ore, cobble, and iron-like metal feeds (plus March grit).
     */
    public static boolean isShatterFeed(Item item) {
        return item == Items.RAW_IRON
                || item == Items.RAW_GOLD
                || item == Items.RAW_COPPER
                || item == Items.IRON_ORE
                || item == Items.DEEPSLATE_IRON_ORE
                || item == Items.GOLD_ORE
                || item == Items.DEEPSLATE_GOLD_ORE
                || item == Items.COPPER_ORE
                || item == Items.DEEPSLATE_COPPER_ORE
                || item == Items.IRON_INGOT
                || item == Items.GOLD_INGOT
                || item == Items.COPPER_INGOT
                || item == Items.IRON_BLOCK
                || item == Items.COBBLESTONE
                || item == Items.COBBLED_DEEPSLATE
                || item == Items.STONE
                || item == ModItems.MARCH_COBBLE.get()
                || item == ModItems.MARCH_ORE.get()
                || item == ModItems.MARCH_STONE.get();
    }

    /** @deprecated use {@link #isShatterFeed(Item)} */
    @Deprecated
    public static boolean isRawOreFeed(Item item) {
        return isShatterFeed(item);
    }

    public static boolean isProcessable(ItemStack stack) {
        return forInput(stack) != null;
    }
}
