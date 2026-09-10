package tk.darrow.tribalpower.pit;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.grit.GritRegistry;
import tk.darrow.tribalpower.item.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the ground can be asked for, and what asking costs (design 3.1 section 7.5).
 *
 * <p>The pit calls up exactly what a silk-touch mining trip produces -- the raw item for a metal, the ore
 * block for a gem -- so Echo Shatter stays load-bearing and the Grit-singers' own craft is not skipped by
 * their own machine.
 *
 * <p>Substrates are renewable on purpose: stone comes from the Stone Font and March Stone from cobble.
 * Neither deepslate nor March Crystal appears here, because both are worldgen-only and would throttle the
 * tier behind a pickaxe.
 */
public enum OreBand {
    /** What any finished pit can reach. */
    COMMON(1, 10, 14, List.of("coal", "copper", "iron")),
    /** The band under the world, and the reason a pit is worth bracing. */
    DEEP(2, 15, 18, List.of("gold", "redstone", "lapis", "quartz")),
    /** Fire's bargain: the Nether's ores without the Nether, at twice the Pulse. Never ancient debris. */
    HOT(2, 20, 36, List.of()),
    /** Spirit's bargain, and where a modded metal the scan discovered ends up. */
    RARE(2, 40, 24, List.of("diamond", "emerald"));

    /** Every material any band names, so RARE can claim what nothing else did. */
    private static final List<String> NAMED = new ArrayList<>();

    static {
        NAMED.addAll(COMMON.materials);
        NAMED.addAll(DEEP.materials);
        NAMED.addAll(RARE.materials);
    }

    private final int patternTier;
    private final int seconds;
    private final int pulsePerSecond;
    private final List<String> materials;

    OreBand(int patternTier, int seconds, int pulsePerSecond, List<String> materials) {
        this.patternTier = patternTier;
        this.seconds = seconds;
        this.pulsePerSecond = pulsePerSecond;
        this.materials = materials;
    }

    public int patternTier() { return patternTier; }
    public int pulsePerSecond() { return pulsePerSecond; }
    public String key() { return name().toLowerCase(Locale.ROOT); }

    public int seconds() {
        return Math.max(1, (int) Math.round(seconds / TribalConfig.pitSpeedMultiplier()));
    }

    /** One thing the band can call up: a discovered material, or a fixed block for the Nether's ores. */
    public record Entry(String material, ItemStack fixed) {
        public ItemStack result(boolean deepslateBand) {
            if (fixed != null) return fixed.copy();
            GritRegistry.Material known = GritRegistry.material(material);
            return known == null ? ItemStack.EMPTY : known.pitResult(deepslateBand);
        }
    }

    /** What this band can call up right now, given whatever materials the tag scan found. */
    public List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        if (this == HOT) {
            out.add(new Entry("quartz", new ItemStack(Items.NETHER_QUARTZ_ORE)));
            out.add(new Entry("gold", new ItemStack(Items.NETHER_GOLD_ORE)));
            return out;
        }
        for (String material : materials) if (GritRegistry.material(material) != null) out.add(new Entry(material, null));
        if (this == RARE) {
            // Anything the scan found that no band claimed is rare by definition: a modded metal is not
            // something the common band should be handing out.
            for (GritRegistry.Material material : GritRegistry.materials())
                if (!NAMED.contains(material.name())) out.add(new Entry(material.name(), null));
        }
        return out;
    }

    /** The band a material belongs to, or null when nothing calls it up. */
    public static OreBand of(String material) {
        if (material == null) return null;
        for (OreBand band : values()) if (band.materials.contains(material)) return band;
        return GritRegistry.material(material) != null ? RARE : null;
    }

    /**
     * Picks what the ground offers this cycle. A sample present in the band weighs six times as much as
     * anything else; a sample the band does not hold is simply ignored (design 3.1 section 7.4).
     */
    public Entry roll(RandomSource random, String sample) {
        List<Entry> entries = entries();
        if (entries.isEmpty()) return null;
        int total = 0;
        for (Entry entry : entries) total += weight(entry, sample);
        int roll = random.nextInt(Math.max(1, total));
        for (Entry entry : entries) {
            roll -= weight(entry, sample);
            if (roll < 0) return entry;
        }
        return entries.getFirst();
    }

    /** True when the sample names something this band actually holds. */
    public boolean biasedBy(String sample) {
        if (sample == null) return false;
        for (Entry entry : entries()) if (entry.material().equals(sample)) return true;
        return false;
    }

    private static int weight(Entry entry, String sample) {
        return entry.material().equals(sample) ? 6 : 1;
    }

    /** True when {@code stack} is substrate for some band, so the sides cannot be jammed with junk. */
    public static boolean isSubstrate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (OreBand band : values())
            for (ItemStack want : band.substrate()) if (ItemStack.isSameItemSameComponents(want, stack)) return true;
        return false;
    }

    /** What one cycle consumes, before the Loom's Threading halves it. */
    public List<ItemStack> substrate() {
        return switch (this) {
            case COMMON -> List.of(new ItemStack(Items.STONE, 2));
            case DEEP, HOT -> List.of(new ItemStack(ModItems.MARCH_STONE.get(), 2));
            case RARE -> List.of(new ItemStack(ModItems.MARCH_STONE.get(), 4), new ItemStack(ModItems.SPIRIT_SHARD.get(), 1));
        };
    }
}
