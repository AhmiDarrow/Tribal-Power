package tk.darrow.tribalpower.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.block.ModBlocks;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);

    // Guide
    public static final DeferredItem<SpiritCodexItem> SPIRIT_CODEX = ITEMS.register(
            "spirit_codex",
            () -> new SpiritCodexItem(new Item.Properties().stacksTo(1))
    );

    // Materials / pulse reagents
    public static final DeferredItem<Item> SPIRIT_SHARD = ITEMS.registerSimpleItem("spirit_shard");
    public static final DeferredItem<Item> BONE_CHIME = ITEMS.registerSimpleItem("bone_chime");
    public static final DeferredItem<Item> COPPER_RESONATOR = ITEMS.registerSimpleItem("copper_resonator");
    public static final DeferredItem<PulseCellItem> PULSE_CELL = ITEMS.register(
            "pulse_cell",
            () -> new PulseCellItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<RitualChalkItem> RITUAL_CHALK = ITEMS.register(
            "ritual_chalk",
            () -> new RitualChalkItem(new Item.Properties().stacksTo(64))
    );

    // Echo stage intermediates — shattered grit → manifested metal (not furnace smelting)
    public static final DeferredItem<Item> ECHO_SHARD = ITEMS.registerSimpleItem("echo_shard");
    public static final DeferredItem<Item> ATTUNED_ECHO = ITEMS.registerSimpleItem("attuned_echo");
    public static final DeferredItem<Item> BOUND_ECHO = ITEMS.registerSimpleItem("bound_echo");
    public static final DeferredItem<Item> MANIFESTED_INGOT = ITEMS.registerSimpleItem("manifested_ingot");

    // Seals
    public static final DeferredItem<Item> BLANK_SEAL = ITEMS.registerSimpleItem("blank_seal");
    public static final DeferredItem<Item> EARTH_SEAL = ITEMS.registerSimpleItem("earth_seal");
    public static final DeferredItem<Item> FIRE_SEAL = ITEMS.registerSimpleItem("fire_seal");
    public static final DeferredItem<Item> WATER_SEAL = ITEMS.registerSimpleItem("water_seal");
    public static final DeferredItem<Item> AIR_SEAL = ITEMS.registerSimpleItem("air_seal");
    public static final DeferredItem<Item> SPIRIT_SEAL = ITEMS.registerSimpleItem("spirit_seal");

    // Spiritgear — Pulse-fueled tools
    public static final DeferredItem<Item> SPIRITGEAR_PICKAXE = ITEMS.register(
            "spiritgear_pickaxe",
            () -> new SpiritgearPickaxeItem(new Item.Properties().durability(512))
    );
    public static final DeferredItem<Item> SPIRITGEAR_AXE = ITEMS.register(
            "spiritgear_axe",
            () -> new SpiritgearAxeItem(new Item.Properties().durability(512))
    );
    public static final DeferredItem<Item> SPIRITGEAR_SHOVEL = ITEMS.register(
            "spiritgear_shovel",
            () -> new SpiritgearShovelItem(new Item.Properties().durability(512))
    );
    public static final DeferredItem<Item> SPIRITGEAR_BLADE = ITEMS.register(
            "spiritgear_blade",
            () -> new SpiritgearBladeItem(new Item.Properties().durability(512))
    );

    // Block items — pulse / lattice
    public static final DeferredItem<BlockItem> DRUMHEART = ITEMS.registerSimpleBlockItem("drumheart", ModBlocks.DRUMHEART);
    public static final DeferredItem<BlockItem> LEY_COLLECTOR = ITEMS.registerSimpleBlockItem("ley_collector", ModBlocks.LEY_COLLECTOR);
    public static final DeferredItem<BlockItem> RESONANCE_TOTEM_EARTH = ITEMS.registerSimpleBlockItem("resonance_totem_earth", ModBlocks.RESONANCE_TOTEM_EARTH);
    public static final DeferredItem<BlockItem> RESONANCE_TOTEM_FIRE = ITEMS.registerSimpleBlockItem("resonance_totem_fire", ModBlocks.RESONANCE_TOTEM_FIRE);
    public static final DeferredItem<BlockItem> RESONANCE_TOTEM_WATER = ITEMS.registerSimpleBlockItem("resonance_totem_water", ModBlocks.RESONANCE_TOTEM_WATER);
    public static final DeferredItem<BlockItem> RESONANCE_TOTEM_AIR = ITEMS.registerSimpleBlockItem("resonance_totem_air", ModBlocks.RESONANCE_TOTEM_AIR);
    public static final DeferredItem<BlockItem> RESONANCE_TOTEM_SPIRIT = ITEMS.registerSimpleBlockItem("resonance_totem_spirit", ModBlocks.RESONANCE_TOTEM_SPIRIT);
    public static final DeferredItem<BlockItem> SONG_BENCH = ITEMS.registerSimpleBlockItem("song_bench", ModBlocks.SONG_BENCH);
    public static final DeferredItem<BlockItem> LATTICE_CONDUCTOR = ITEMS.registerSimpleBlockItem("lattice_conductor", ModBlocks.LATTICE_CONDUCTOR);

    // Echo stage stations (markers / future dedicated processors; Song Bench does live refinement)
    public static final DeferredItem<BlockItem> ECHO_SHATTER = ITEMS.registerSimpleBlockItem("echo_shatter", ModBlocks.ECHO_SHATTER);
    public static final DeferredItem<BlockItem> ECHO_ATTUNE = ITEMS.registerSimpleBlockItem("echo_attune", ModBlocks.ECHO_ATTUNE);
    public static final DeferredItem<BlockItem> ECHO_BIND = ITEMS.registerSimpleBlockItem("echo_bind", ModBlocks.ECHO_BIND);
    public static final DeferredItem<BlockItem> ECHO_MANIFEST = ITEMS.registerSimpleBlockItem("echo_manifest", ModBlocks.ECHO_MANIFEST);

    // Storage / rites / portal
    public static final DeferredItem<BlockItem> ANCESTRAL_CACHE = ITEMS.registerSimpleBlockItem("ancestral_cache", ModBlocks.ANCESTRAL_CACHE);
    public static final DeferredItem<BlockItem> DEEP_CACHE = ITEMS.registerSimpleBlockItem("deep_cache", ModBlocks.DEEP_CACHE);
    public static final DeferredItem<BlockItem> RITE_PEDESTAL = ITEMS.registerSimpleBlockItem("rite_pedestal", ModBlocks.RITE_PEDESTAL);
    public static final DeferredItem<BlockItem> GATE_DRUM = ITEMS.registerSimpleBlockItem("gate_drum", ModBlocks.GATE_DRUM);
    public static final DeferredItem<BlockItem> SPIRIT_DOOR = ITEMS.registerSimpleBlockItem("spirit_door", ModBlocks.SPIRIT_DOOR);

    // March
    public static final DeferredItem<BlockItem> MARCH_STONE = ITEMS.registerSimpleBlockItem("march_stone", ModBlocks.MARCH_STONE);
    public static final DeferredItem<BlockItem> MARCH_COBBLE = ITEMS.registerSimpleBlockItem("march_cobble", ModBlocks.MARCH_COBBLE);
    public static final DeferredItem<BlockItem> MARCH_SOIL = ITEMS.registerSimpleBlockItem("march_soil", ModBlocks.MARCH_SOIL);
    public static final DeferredItem<BlockItem> MARCH_GRASS = ITEMS.registerSimpleBlockItem("march_grass", ModBlocks.MARCH_GRASS);
    public static final DeferredItem<BlockItem> MARCH_LOG = ITEMS.registerSimpleBlockItem("march_log", ModBlocks.MARCH_LOG);
    public static final DeferredItem<BlockItem> MARCH_PLANKS = ITEMS.registerSimpleBlockItem("march_planks", ModBlocks.MARCH_PLANKS);
    public static final DeferredItem<BlockItem> MARCH_LEAVES = ITEMS.registerSimpleBlockItem("march_leaves", ModBlocks.MARCH_LEAVES);
    public static final DeferredItem<BlockItem> MARCH_LEAF = ITEMS.registerSimpleBlockItem("march_leaf", ModBlocks.MARCH_LEAF);
    public static final DeferredItem<BlockItem> MARCH_ORE = ITEMS.registerSimpleBlockItem("march_ore", ModBlocks.MARCH_ORE);
    public static final DeferredItem<BlockItem> MARCH_CRYSTAL = ITEMS.registerSimpleBlockItem("march_crystal", ModBlocks.MARCH_CRYSTAL);
    public static final DeferredItem<BlockItem> SPIRIT_REED = ITEMS.registerSimpleBlockItem("spirit_reed", ModBlocks.SPIRIT_REED);
    public static final DeferredItem<BlockItem> ECHO_BLOOM = ITEMS.registerSimpleBlockItem("echo_bloom", ModBlocks.ECHO_BLOOM);

    private ModItems() {}
}
