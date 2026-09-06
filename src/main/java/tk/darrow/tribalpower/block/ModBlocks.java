package tk.darrow.tribalpower.block;

import net.minecraft.world.level.block.Block;
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

    public static final DeferredBlock<Block> LEY_COLLECTOR = BLOCKS.registerSimpleBlock(
            "ley_collector",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    );

    // Lattice
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_EARTH = totem("resonance_totem_earth", Attunement.EARTH, MapColor.DIRT);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_FIRE = totem("resonance_totem_fire", Attunement.FIRE, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_WATER = totem("resonance_totem_water", Attunement.WATER, MapColor.WATER);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_AIR = totem("resonance_totem_air", Attunement.AIR, MapColor.WOOL);
    public static final DeferredBlock<ResonanceTotemBlock> RESONANCE_TOTEM_SPIRIT = totem("resonance_totem_spirit", Attunement.SPIRIT, MapColor.COLOR_PURPLE);

    public static final DeferredBlock<SongBenchBlock> SONG_BENCH = BLOCKS.register(
            "song_bench",
            () -> new SongBenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion())
    );

    public static final DeferredBlock<Block> LATTICE_CONDUCTOR = BLOCKS.registerSimpleBlock(
            "lattice_conductor",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F)
                    .sound(SoundType.COPPER)
    );

    // Echo stages (ore refinement path)
    public static final DeferredBlock<Block> ECHO_SHATTER = echo("echo_shatter", MapColor.STONE);
    public static final DeferredBlock<Block> ECHO_ATTUNE = echo("echo_attune", MapColor.COLOR_CYAN);
    public static final DeferredBlock<Block> ECHO_BIND = echo("echo_bind", MapColor.COLOR_PURPLE);
    public static final DeferredBlock<Block> ECHO_MANIFEST = echo("echo_manifest", MapColor.GOLD);

    // Storage
    public static final DeferredBlock<AncestralCacheBlock> ANCESTRAL_CACHE = BLOCKS.register(
            "ancestral_cache",
            () -> new AncestralCacheBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD))
    );

    public static final DeferredBlock<Block> DEEP_CACHE = BLOCKS.registerSimpleBlock(
            "deep_cache",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0F, 12.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
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

    public static final DeferredBlock<Block> SPIRIT_REED = BLOCKS.register(
            "spirit_reed",
            () -> new MarchPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion())
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
        return BLOCKS.registerSimpleBlock(id, BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }

    private ModBlocks() {}
}
