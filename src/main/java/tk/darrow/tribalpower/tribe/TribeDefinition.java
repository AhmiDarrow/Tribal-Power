package tk.darrow.tribalpower.tribe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import org.joml.Vector3f;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.item.CreatureItems;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The nine tribes of the Loom (design 3.0 §2). Order is the wire contract: {@code Tribe} NBT is the ordinal.
 */
public enum TribeDefinition {
    SOIL("soil", "Pad-keepers", "Soil", Attunement.EARTH, 0x8b5a2b,
            "We never called it dirt. We called it what was left, and we kept it warm."),
    STONE("stone", "Grit-singers", "Stone", Attunement.EARTH, 0x7d8791,
            "The mesh does not find the ore. The mesh gives the ore somewhere to land."),
    SPROUT("sprout", "Rootbinders", "Sprout", Attunement.WATER, 0x4f9a5a,
            "Roots are the only rope the void respects."),
    CLAW("claw", "Edge-walkers", "Claw", Attunement.FIRE, 0xb8512f,
            "Boots first. Then the bridge. Then the courage; it arrives on its own."),
    SPARK("spark", "Drumhearts", "Spark", Attunement.FIRE, 0xe0a32d,
            "The drum is not loud. The drum is steady. Be the drum."),
    CLOCK("clock", "Pattern-weavers", "Clock", Attunement.AIR, 0x4a7fb5,
            "A factory is a song that has stopped needing the singer."),
    SWARM("swarm", "Colony-keepers", "Swarm", Attunement.AIR, 0xd7b23c,
            "You do not own a hive. You are on good terms with it."),
    SIGIL("sigil", "Seal-carvers", "Sigil", Attunement.SPIRIT, 0x8a5fc7,
            "Spirit goes where it is asked politely and stays where it is fed."),
    SPINDLE("spindle", "Loom-stitchers", "Spindle", Attunement.LOOM, 0x62d1c9,
            "The Loom was never one thread. It was nine agreeing.");

    /** Item NBT / block entity NBT key carrying the tribe ordinal. */
    public static final String NBT_KEY = "Tribe";
    public static final TagKey<Item> RAW_MATERIALS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials"));
    public static final TagKey<Item> ORES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    public static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds"));
    public static final TagKey<Item> FLOWERS = ItemTags.FLOWERS;
    public static final TagKey<Item> SAPLINGS = ItemTags.SAPLINGS;
    public static final TagKey<Item> DIRT = ItemTags.DIRT;

    /** Offer tier: which standing rank unlocks the trade. */
    public record Offer(TribeRank rank, Supplier<ItemStack> cost, Supplier<ItemStack> costB, Supplier<ItemStack> result, int maxUses) {
        public static Offer of(TribeRank rank, ItemLike cost, int count, ItemLike result, int resultCount) {
            return new Offer(rank, () -> new ItemStack(cost, count), null, () -> new ItemStack(result, resultCount), 12);
        }
        public static Offer of(TribeRank rank, ItemLike cost, int count, ItemLike costB, int countB, ItemLike result, int resultCount) {
            return new Offer(rank, () -> new ItemStack(cost, count), () -> new ItemStack(costB, countB), () -> new ItemStack(result, resultCount), 8);
        }
        public static Offer stack(TribeRank rank, ItemLike cost, int count, Supplier<ItemStack> result) {
            return new Offer(rank, () -> new ItemStack(cost, count), null, result, 6);
        }
    }

    private final String id;
    private final String displayName;
    private final String strand;
    private final Attunement attunement;
    private final int colour;
    private final String marginLine;

    TribeDefinition(String id, String displayName, String strand, Attunement attunement, int colour, String marginLine) {
        this.id = id;
        this.displayName = displayName;
        this.strand = strand;
        this.attunement = attunement;
        this.colour = colour;
        this.marginLine = marginLine;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String strand() { return strand; }
    public Attunement attunement() { return attunement; }
    /** RGB colour without alpha. */
    public int colour() { return colour; }
    public String marginLine() { return marginLine; }
    public String translationKey() { return "tribe.tribalpower." + id; }
    public String marginKey() { return "tribe.tribalpower." + id + ".margin"; }
    public net.minecraft.network.chat.MutableComponent displayNameComponent() { return net.minecraft.network.chat.Component.translatable(translationKey()); }
    public net.minecraft.network.chat.MutableComponent marginComponent() { return net.minecraft.network.chat.Component.translatable(marginKey()); }

    public Vector3f particleColour() {
        return new Vector3f(((colour >> 16) & 255) / 255F, ((colour >> 8) & 255) / 255F, (colour & 255) / 255F);
    }

    public static TribeDefinition byOrdinal(int ordinal) {
        TribeDefinition[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }

    public static TribeDefinition byId(String id) {
        for (TribeDefinition t : values()) if (t.id.equals(id)) return t;
        return SOIL;
    }

    // ---- item stamping (Tribe Mark, hearth item, banner item, kinship totem item) ----

    /** Tribe stamped into an item's CUSTOM_DATA, or {@code null} when absent. */
    public static TribeDefinition of(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(NBT_KEY)) return null;
        return byOrdinal(data.getUnsafe().getInt(NBT_KEY)); // read-only: no need to copy the tag per item-model predicate call
    }

    public static TribeDefinition ofOrDefault(ItemStack stack) {
        TribeDefinition t = of(stack);
        return t == null ? SOIL : t;
    }

    public static ItemStack stamp(ItemStack stack, TribeDefinition tribe) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(NBT_KEY, tribe.ordinal()));
        return stack;
    }

    public ItemStack stamped(ItemLike item) {
        return stamp(new ItemStack(item), this);
    }

    // ---- offerings ----

    private static ItemStack reagent(CreatureProfile profile) {
        return new ItemStack(CreatureItems.REAGENTS.get(profile).get());
    }

    private static boolean isReagent(ItemStack stack, CreatureProfile profile) {
        return stack.is(CreatureItems.REAGENTS.get(profile).get());
    }

    /** The tribe's reagent-tier offering (+8 standing). */
    public boolean reagent(ItemStack stack) {
        return switch (this) {
            case SOIL -> stack.is(ModItems.ECHO_SHARD.get());
            case STONE -> stack.is(ModItems.ATTUNED_ECHO.get());
            case SPROUT -> isReagent(stack, CreatureProfile.MOSSBACK);
            case CLAW -> isReagent(stack, CreatureProfile.RIFT_HOUND);
            case SPARK -> stack.is(ModItems.BONE_CHIME.get());
            case CLOCK -> isReagent(stack, CreatureProfile.STORM_MOTH);
            case SWARM -> isReagent(stack, CreatureProfile.LANTERN_FOX);
            case SIGIL -> stack.is(ModItems.SPIRIT_SHARD.get());
            case SPINDLE -> stack.is(ModItems.LOOM_THREAD.get());
        };
    }

    /** Favoured offering (+3 standing); reagents are favoured too. */
    public boolean favoured(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (reagent(stack)) return true;
        return switch (this) {
            case SOIL -> stack.is(DIRT) || stack.is(Items.MOSS_BLOCK) || stack.is(Items.BREAD)
                    || stack.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_SOIL.get().asItem())
                    || stack.is(tk.darrow.tribalpower.block.ModBlocks.MARCH_MOSS.get().asItem());
            case STONE -> stack.is(RAW_MATERIALS) || stack.is(ORES) || stack.is(ModItems.IRON_GRIT.get())
                    || stack.is(ModItems.GOLD_GRIT.get()) || stack.is(ModItems.COPPER_GRIT.get());
            case SPROUT -> stack.is(SAPLINGS) || stack.is(SEEDS) || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.MOSS_BLOCK)
                    || stack.is(tk.darrow.tribalpower.block.ModBlocks.SPIRIT_REED.get().asItem());
            case CLAW -> stack.is(Items.LEATHER) || stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON);
            case SPARK -> stack.is(Items.COPPER_INGOT) || stack.is(Items.RAW_COPPER) || stack.is(ModItems.COPPER_RESONATOR.get())
                    || (stack.getItem() instanceof PulseCellItem && PulseCellItem.getPulse(stack) > 0);
            case CLOCK -> stack.is(Items.REDSTONE) || stack.is(Items.CLOCK) || stack.is(Items.REPEATER) || stack.is(Items.COMPARATOR);
            case SWARM -> stack.is(Items.HONEY_BOTTLE) || stack.is(Items.HONEYCOMB) || stack.is(Items.HONEY_BLOCK) || stack.is(FLOWERS);
            case SIGIL -> stack.is(ModItems.BLANK_SEAL.get()) || stack.is(ModItems.EARTH_SEAL.get()) || stack.is(ModItems.FIRE_SEAL.get())
                    || stack.is(ModItems.WATER_SEAL.get()) || stack.is(ModItems.AIR_SEAL.get()) || stack.is(ModItems.SPIRIT_SEAL.get())
                    || stack.is(ModItems.LOOM_SEAL.get());
            case SPINDLE -> stack.is(ModItems.MARCH_CRYSTAL.get()) || stack.is(Items.COMPASS)
                    || stack.is(ModItems.WAYSTONE_COMPASS.get()) || stack.is(ModItems.HORIZON_COMPASS.get());
        };
    }

    /** Standing granted by one offering of {@code stack}, or 0 when it is not accepted. */
    public int offeringValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (reagent(stack)) return TribeStanding.GAIN_REAGENT;
        if (favoured(stack)) return TribeStanding.GAIN_FAVOURED;
        if (stack.has(DataComponents.FOOD)) return TribeStanding.GAIN_FOOD;
        return 0;
    }

    /** Seal item matching this tribe's voice. */
    public Item seal() {
        return switch (attunement) {
            case EARTH -> ModItems.EARTH_SEAL.get();
            case FIRE -> ModItems.FIRE_SEAL.get();
            case WATER -> ModItems.WATER_SEAL.get();
            case AIR -> ModItems.AIR_SEAL.get();
            case SPIRIT -> ModItems.SPIRIT_SEAL.get();
            case LOOM -> ModItems.LOOM_SEAL.get();
        };
    }

    /** Resonance totem block item matching this tribe's voice. */
    public Item totem() {
        return switch (attunement) {
            case EARTH -> ModItems.RESONANCE_TOTEM_EARTH.get();
            case FIRE -> ModItems.RESONANCE_TOTEM_FIRE.get();
            case WATER -> ModItems.RESONANCE_TOTEM_WATER.get();
            case AIR -> ModItems.RESONANCE_TOTEM_AIR.get();
            case SPIRIT -> ModItems.RESONANCE_TOTEM_SPIRIT.get();
            case LOOM -> ModItems.RESONANCE_TOTEM_LOOM.get();
        };
    }

    /** Six offers: two per rank Guest / Friend / Kin. */
    public List<Offer> trades() {
        return switch (this) {
            case SOIL -> List.of(
                    Offer.of(TribeRank.GUEST, Items.BREAD, 4, ModItems.ECHO_SHARD.get(), 2),
                    Offer.of(TribeRank.GUEST, Items.MOSS_BLOCK, 6, ModItems.MARCH_MOSS.get(), 4),
                    Offer.of(TribeRank.FRIEND, ModItems.ECHO_SHARD.get(), 6, ModItems.MARCH_SOIL.get(), 8),
                    Offer.of(TribeRank.FRIEND, Items.BREAD, 8, ModItems.ATTUNED_ECHO.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.ATTUNED_ECHO.get(), 4, ModItems.ANCESTRAL_CACHE.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 2, Items.MOSS_BLOCK, 8, ModItems.DEEP_CACHE.get(), 1));
            case STONE -> List.of(
                    Offer.of(TribeRank.GUEST, Items.RAW_IRON, 4, ModItems.ECHO_SHARD.get(), 3),
                    Offer.of(TribeRank.GUEST, Items.RAW_COPPER, 6, ModItems.COPPER_GRIT.get(), 4),
                    Offer.of(TribeRank.FRIEND, ModItems.IRON_GRIT.get(), 6, ModItems.ATTUNED_ECHO.get(), 2),
                    Offer.of(TribeRank.FRIEND, Items.RAW_GOLD, 3, ModItems.GOLD_GRIT.get(), 4),
                    Offer.of(TribeRank.KIN, ModItems.ATTUNED_ECHO.get(), 6, ModItems.BOUND_ECHO.get(), 2),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 4, ModItems.ECHO_SHATTER.get(), 1));
            case SPROUT -> List.of(
                    Offer.of(TribeRank.GUEST, Items.OAK_SAPLING, 4, ModItems.SPIRIT_REED.get(), 3),
                    Offer.of(TribeRank.GUEST, Items.WHEAT_SEEDS, 12, Items.BONE_MEAL, 8),
                    Offer.of(TribeRank.FRIEND, ModItems.SPIRIT_REED.get(), 6, ModItems.MARCH_LEAF.get(), 4),
                    Offer.stack(TribeRank.FRIEND, Items.MOSS_BLOCK, 8, () -> reagent(CreatureProfile.MOSSBACK)),
                    Offer.of(TribeRank.KIN, ModItems.ECHO_SHARD.get(), 8, ModItems.MARCH_LOG.get(), 6),
                    Offer.of(TribeRank.KIN, ModItems.SPIRITWEAVE.get(), 2, ModItems.ECHO_BLOOM.get(), 2));
            case CLAW -> List.of(
                    Offer.of(TribeRank.GUEST, Items.LEATHER, 6, ModItems.ECHO_SHARD.get(), 2),
                    Offer.of(TribeRank.GUEST, Items.IRON_INGOT, 4, Items.LEATHER_BOOTS, 1),
                    Offer.of(TribeRank.FRIEND, ModItems.ATTUNED_ECHO.get(), 3, Items.LEATHER, 4, ModItems.SPIRITWEAVE_BOOTS.get(), 1),
                    Offer.stack(TribeRank.FRIEND, Items.IRON_INGOT, 8, () -> reagent(CreatureProfile.RIFT_HOUND)),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 3, ModItems.SPIRITGEAR_BLADE.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 3, ModItems.SPIRITGEAR_PICKAXE.get(), 1));
            case SPARK -> List.of(
                    Offer.of(TribeRank.GUEST, Items.COPPER_INGOT, 6, ModItems.BONE_CHIME.get(), 1),
                    Offer.of(TribeRank.GUEST, Items.COPPER_INGOT, 3, ModItems.COPPER_RESONATOR.get(), 1),
                    Offer.of(TribeRank.FRIEND, ModItems.ATTUNED_ECHO.get(), 2, ModItems.PULSE_CELL.get(), 1),
                    Offer.stack(TribeRank.FRIEND, ModItems.BONE_CHIME.get(), 2, () -> PulseCellItem.createFilled(PulseCellItem.CAPACITY)),
                    Offer.of(TribeRank.KIN, ModItems.RESONANT_CORE.get(), 1, ModItems.GREATER_PULSE_CELL.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 4, ModItems.DRUMHEART.get(), 1));
            case CLOCK -> List.of(
                    Offer.of(TribeRank.GUEST, Items.REDSTONE, 8, ModItems.ECHO_SHARD.get(), 2),
                    Offer.of(TribeRank.GUEST, Items.CLOCK, 1, Items.REPEATER, 4),
                    Offer.of(TribeRank.FRIEND, ModItems.ATTUNED_ECHO.get(), 2, Items.COMPARATOR, 4),
                    Offer.stack(TribeRank.FRIEND, Items.REDSTONE, 16, () -> reagent(CreatureProfile.STORM_MOTH)),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 3, ModItems.LATTICE_CONDUCTOR.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 2, ModItems.ITEM_RELAY.get(), 1));
            case SWARM -> List.of(
                    Offer.of(TribeRank.GUEST, Items.HONEYCOMB, 4, ModItems.ECHO_SHARD.get(), 2),
                    Offer.of(TribeRank.GUEST, Items.HONEY_BOTTLE, 2, ModItems.LEY_THISTLE.get(), 2),
                    Offer.of(TribeRank.FRIEND, Items.HONEYCOMB, 8, ModItems.ECHO_BLOOM.get(), 2),
                    Offer.stack(TribeRank.FRIEND, Items.HONEY_BLOCK, 2, () -> reagent(CreatureProfile.LANTERN_FOX)),
                    Offer.of(TribeRank.KIN, ModItems.ATTUNED_ECHO.get(), 4, ModItems.LEY_COLLECTOR.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 2, ModItems.SPIRITWEAVE.get(), 3));
            case SIGIL -> List.of(
                    Offer.of(TribeRank.GUEST, ModItems.SPIRIT_SHARD.get(), 2, ModItems.BLANK_SEAL.get(), 1),
                    Offer.of(TribeRank.GUEST, ModItems.ECHO_SHARD.get(), 4, ModItems.RITUAL_CHALK.get(), 4),
                    Offer.of(TribeRank.FRIEND, ModItems.BLANK_SEAL.get(), 1, ModItems.SPIRIT_SHARD.get(), 2, ModItems.SPIRIT_SEAL.get(), 1),
                    // Seal-carvers sell a Rite Tablet at Friend rank as a shortcut past the reagent hunt (design 3.0 §4).
                    Offer.of(TribeRank.FRIEND, ModItems.ATTUNED_ECHO.get(), 3, ModItems.SPIRIT_SHARD.get(), 2,
                            tk.darrow.tribalpower.rite.world.WorldRiteRegistry.TABLETS.get(tk.darrow.tribalpower.rite.world.WorldRite.STILL_NIGHT).get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 3, ModItems.RITUAL_BRAZIER.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 4, ModItems.RESONANCE_TOTEM_SPIRIT.get(), 1));
            case SPINDLE -> List.of(
                    Offer.of(TribeRank.GUEST, ModItems.MARCH_CRYSTAL.get(), 4, ModItems.ECHO_SHARD.get(), 3),
                    Offer.of(TribeRank.GUEST, Items.COMPASS, 1, ModItems.MARCH_PLANKS.get(), 8),
                    Offer.of(TribeRank.FRIEND, ModItems.MARCH_CRYSTAL.get(), 6, ModItems.LOOM_THREAD.get(), 1),
                    Offer.of(TribeRank.FRIEND, ModItems.ATTUNED_ECHO.get(), 4, ModItems.WAYSTONE_COMPASS.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.BOUND_ECHO.get(), 4, ModItems.LOOM_THREAD.get(), 1, ModItems.HORIZON_COMPASS.get(), 1),
                    Offer.of(TribeRank.KIN, ModItems.LOOM_THREAD.get(), 2, ModItems.LOOM_SEAL.get(), 1));
        };
    }

    /** Offers unlocked at {@code rank} (inclusive). */
    public List<Offer> tradesFor(TribeRank rank) {
        return trades().stream().filter(o -> rank.ordinal() >= o.rank().ordinal()).toList();
    }

    public static Predicate<ItemStack> favouredPredicate(TribeDefinition tribe) {
        return tribe::favoured;
    }
}
