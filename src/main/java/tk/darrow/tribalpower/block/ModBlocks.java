package tk.darrow.tribalpower.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);

    // Pulse / power
    public static final DeferredBlock<DrumheartBlock> DRUMHEART = BLOCKS.register(
            "drumheart",
            () -> new DrumheartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<LeyCollectorBlock> LEY_COLLECTOR = BLOCKS.register(
            "ley_collector",
            () -> new LeyCollectorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<PulseResonatorBlock> PULSE_RESONATOR = BLOCKS.register(
            "pulse_resonator",
            () -> new PulseResonatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(PulseResonatorBlock.LIT) ? 8 : 0))
    );

    // Lattice
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_EARTH = totem("resonance_totem_earth", Attunement.EARTH, MapColor.DIRT);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_FIRE = totem("resonance_totem_fire", Attunement.FIRE, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_WATER = totem("resonance_totem_water", Attunement.WATER, MapColor.WATER);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_AIR = totem("resonance_totem_air", Attunement.AIR, MapColor.WOOL);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_SPIRIT = totem("resonance_totem_spirit", Attunement.SPIRIT, MapColor.COLOR_PURPLE);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_LOOM = totem("resonance_totem_loom", Attunement.LOOM, MapColor.DIAMOND);

    public static final DeferredBlock<SongBenchBlock> SONG_BENCH = BLOCKS.register(
            "song_bench",
            () -> new SongBenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
    );

    public static final DeferredBlock<LatticeConductorBlock> LATTICE_CONDUCTOR = BLOCKS.register(
            "lattice_conductor",
            () -> new LatticeConductorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops())
    );

    // Echo stages (ore refinement path)
    public static final DeferredBlock<Block> ECHO_SHATTER = echo("echo_shatter", MapColor.STONE);
    public static final DeferredBlock<Block> ECHO_ATTUNE = echo("echo_attune", MapColor.COLOR_CYAN);
    public static final DeferredBlock<Block> ECHO_BIND = echo("echo_bind", MapColor.COLOR_PURPLE);
    public static final DeferredBlock<Block> ECHO_MANIFEST = echo("echo_manifest", MapColor.GOLD);
    public static final DeferredBlock<Block> ECHO_UNWEAVE = echo("echo_unweave", MapColor.DIAMOND);

    // Storage
    public static final DeferredBlock<AncestralCacheBlock> ANCESTRAL_CACHE = BLOCKS.register(
            "ancestral_cache",
            () -> new AncestralCacheBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD))
    );

    public static final DeferredBlock<DeepCacheBlock> DEEP_CACHE = BLOCKS.register(
            "deep_cache",
            () -> new DeepCacheBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0F, 12.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    // Rites
    public static final DeferredBlock<RitePedestalBlock> RITE_PEDESTAL = BLOCKS.register(
            "rite_pedestal",
            () -> new RitePedestalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion())
    );

    // Portal
    public static final DeferredBlock<GateDrumBlock> GATE_DRUM = BLOCKS.register(
            "gate_drum",
            () -> new GateDrumBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 20.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(s -> 7))
    );

    public static final DeferredBlock<Block> SPIRIT_DOOR = BLOCKS.registerSimpleBlock(
            "spirit_door",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(4.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(s -> 5)
                    .noOcclusion()
    );

    // The March world blocks
    public static final DeferredBlock<Block> MARCH_STONE = BLOCKS.registerSimpleBlock(
            "march_stone",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> MARCH_COBBLE = BLOCKS.registerSimpleBlock(
            "march_cobble",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> MARCH_SOIL = BLOCKS.registerSimpleBlock(
            "march_soil",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)
    );

    public static final DeferredBlock<Block> MARCH_GRASS = BLOCKS.registerSimpleBlock(
            "march_grass",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
    );

    public static final DeferredBlock<Block> MARCH_MOSS = BLOCKS.registerSimpleBlock(
            "march_moss",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.4F)
                    .sound(SoundType.MOSS)
    );

    public static final DeferredBlock<Block> MARCH_LOG = BLOCKS.registerSimpleBlock(
            "march_log",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
    );

    public static final DeferredBlock<Block> MARCH_PLANKS = BLOCKS.registerSimpleBlock(
            "march_planks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
    );

    public static final DeferredBlock<Block> MARCH_LEAVES = BLOCKS.register(
            "march_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isViewBlocking((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false))
    );

    public static final DeferredBlock<Block> MARCH_LEAF = BLOCKS.register(
            "march_leaf",
            () -> new MarchPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isViewBlocking((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false))
    );

    public static final DeferredBlock<Block> MARCH_ORE = BLOCKS.registerSimpleBlock(
            "march_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<MarchCrystalBlock> MARCH_CRYSTAL = BLOCKS.register(
            "march_crystal",
            () -> new MarchCrystalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.2F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(s -> 10)
                    .noOcclusion())
    );

    public static final DeferredBlock<Block> SPIRIT_REED = BLOCKS.register(
            "spirit_reed",
            () -> new MarchPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion())
    );

    public static final DeferredBlock<EchoBloomBlock> ECHO_BLOOM = BLOCKS.register(
            "echo_bloom",
            () -> new EchoBloomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(s -> 4))
    );

    public static final DeferredBlock<Block> LEY_THISTLE = BLOCKS.register(
            "ley_thistle",
            () -> new MarchPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(s -> 2))
    );

    private static DeferredBlock<ResonanceTotemBlock> totem(String id, Attunement attunement, MapColor color) {
        return BLOCKS.register(id, () -> new ResonanceTotemBlock(attunement, BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .lightLevel(s -> 4)
                .noOcclusion()));
    }

    private static DeferredBlock<Block> echo(String id, MapColor color) {
        return BLOCKS.register(id, () -> new EchoStationBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()));
    }

    private ModBlocks() {}

    private static DeferredBlock<LatticeUtilityBlock> utility(String name) {
        return BLOCKS.register(name, () -> new LatticeUtilityBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3F).sound(SoundType.COPPER).noOcclusion()));
    }
    public static final DeferredBlock<LatticeUtilityBlock> ITEM_RELAY = utility("item_relay");
    public static final DeferredBlock<LatticeUtilityBlock> FLUID_RELAY = utility("fluid_relay");
    public static final DeferredBlock<LatticeUtilityBlock> LONGREACH_ITEM_RELAY = utility("longreach_item_relay");
    public static final DeferredBlock<LatticeUtilityBlock> LONGREACH_FLUID_RELAY = utility("longreach_fluid_relay");
    public static final DeferredBlock<LatticeUtilityBlock> ASTRAL_ITEM_RELAY = utility("astral_item_relay");
    public static final DeferredBlock<LatticeUtilityBlock> ASTRAL_FLUID_RELAY = utility("astral_fluid_relay");
    public static final DeferredBlock<LatticeUtilityBlock> PULSE_ADAPTER = utility("pulse_adapter");
    public static final DeferredBlock<LatticeUtilityBlock> SPIRIT_CISTERN = utility("spirit_cistern");

    public static final DeferredBlock<RitualBrazierBlock> RITUAL_BRAZIER = BLOCKS.register("ritual_brazier", () ->
            new RitualBrazierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3F).sound(SoundType.COPPER).lightLevel(s -> 5).noOcclusion()));
}
