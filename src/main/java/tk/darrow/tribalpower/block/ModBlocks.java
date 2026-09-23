package tk.darrow.tribalpower.block;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
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
                    .sound(SoundType.WOOD))
    );

    public static final DeferredBlock<LeyCollectorBlock> LEY_COLLECTOR = BLOCKS.register(
            "ley_collector",
            () -> new LeyCollectorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<PulseResonatorBlock> PULSE_RESONATOR = BLOCKS.register(
            "pulse_resonator",
            () -> new PulseResonatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
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
                    .sound(SoundType.COPPER))
    );

    // Echo stages (ore refinement path)
    public static final DeferredBlock<Block> ECHO_SHATTER = echo("echo_shatter", MapColor.STONE);
    public static final DeferredBlock<Block> ECHO_ATTUNE = echo("echo_attune", MapColor.COLOR_CYAN);
    public static final DeferredBlock<Block> ECHO_BIND = echo("echo_bind", MapColor.COLOR_PURPLE);
    public static final DeferredBlock<Block> ECHO_MANIFEST = echo("echo_manifest", MapColor.GOLD);
    public static final DeferredBlock<Block> ECHO_UNWEAVE = echo("echo_unweave", MapColor.DIAMOND);
    public static final DeferredBlock<Block> EMBER_KILN = echo("ember_kiln", MapColor.COLOR_ORANGE);

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
                    .sound(SoundType.STONE))
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

    public static final DeferredBlock<SpiritDoorBlock> SPIRIT_DOOR = BLOCKS.register(
            "spirit_door",
            () -> new SpiritDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(4.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(s -> 5)
                    .noOcclusion())
    );

    // The Glimmer Ridge: a mineral mountain of the March. Moonstone is the body of the ridge,
    // moss agate the seams running through it.
    public static final DeferredBlock<Block> MOONSTONE = BLOCKS.registerSimpleBlock(
            "moonstone",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<MarchQuartzBlock> MARCH_QUARTZ = BLOCKS.register(
            "march_quartz", () -> new MarchQuartzBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(1.0F)
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(s -> 2)
                    .noOcclusion()
                    .noCollission()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final DeferredBlock<Block> MOSS_AGATE = BLOCKS.registerSimpleBlock(
            "moss_agate",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1.6F, 6.0F)
                    .sound(SoundType.CALCITE)
                    .requiresCorrectToolForDrops()
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

    public static final DeferredBlock<MarchFarmlandBlock> MARCH_FARMLAND = BLOCKS.register(
            "march_farmland",
            () -> new MarchFarmlandBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .randomTicks()
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true))
    );

    public static final DeferredBlock<MarchPathBlock> MARCH_PATH = BLOCKS.register(
            "march_path",
            () -> new MarchPathBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.65F)
                    .sound(SoundType.GRASS)
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true))
    );

    public static final TreeGrower MARCH_GROWER = new TreeGrower(
            "march",
            java.util.Optional.empty(),
            java.util.Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "march_tree"))),
            java.util.Optional.empty()
    );

    public static final DeferredBlock<MarchGrassBlock> MARCH_GRASS = BLOCKS.register(
            "march_grass",
            () -> new MarchGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .randomTicks()
                    .ignitedByLava())
    );

    public static final DeferredBlock<Block> MARCH_MOSS = BLOCKS.registerSimpleBlock(
            "march_moss",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.4F)
                    .sound(SoundType.MOSS)
    );

    public static final DeferredBlock<Block> MARCH_LOG = BLOCKS.register(
            "march_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava())
    );

    public static final DeferredBlock<Block> MARCH_PLANKS = BLOCKS.registerSimpleBlock(
            "march_planks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
    );

    public static final DeferredBlock<Block> MARCH_LEAVES = BLOCKS.register(
            "march_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .isViewBlocking((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false))
    );

    public static final DeferredBlock<Block> MARCH_LEAF = BLOCKS.register(
            "march_leaf",
            () -> new MarchPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava()
                    .isViewBlocking((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false))
    );

    public static final DeferredBlock<MarchSaplingBlock> MARCH_SAPLING = BLOCKS.register(
            "march_sapling",
            () -> new MarchSaplingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY))
    );

    public static final DeferredBlock<Block> MARCH_ORE = BLOCKS.register(
            "march_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
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
                    .noOcclusion()
                    .replaceable()
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava())
    );

    public static final DeferredBlock<EchoBloomBlock> ECHO_BLOOM = BLOCKS.register(
            "echo_bloom",
            () -> new EchoBloomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava()
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
                    .replaceable()
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava())
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
                .sound(SoundType.STONE)));
    }

    private ModBlocks() {}

    private static DeferredBlock<LatticeUtilityBlock> utility(String name) {
        return BLOCKS.register(name, () -> new LatticeUtilityBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3F).sound(SoundType.COPPER).noOcclusion()));
    }
    private static DeferredBlock<RelayBlock> relay(String name) {
        return BLOCKS.register(name, () -> new RelayBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3F).sound(SoundType.COPPER).noOcclusion()));
    }
    public static final DeferredBlock<RelayBlock> ITEM_RELAY = relay("item_relay");
    public static final DeferredBlock<RelayBlock> FLUID_RELAY = relay("fluid_relay");
    public static final DeferredBlock<RelayBlock> LONGREACH_ITEM_RELAY = relay("longreach_item_relay");
    public static final DeferredBlock<RelayBlock> LONGREACH_FLUID_RELAY = relay("longreach_fluid_relay");
    public static final DeferredBlock<RelayBlock> ASTRAL_ITEM_RELAY = relay("astral_item_relay");
    public static final DeferredBlock<RelayBlock> ASTRAL_FLUID_RELAY = relay("astral_fluid_relay");
    public static final DeferredBlock<LatticeUtilityBlock> PULSE_ADAPTER = utility("pulse_adapter");
    /** FE back into Pulse. The Harmonic Energizer's opposite number. */
    public static final DeferredBlock<LatticeUtilityBlock> LATTICE_CONVERTER = utility("lattice_converter");
    /**
     * A crafting table that keeps its grid, two blocks wide, with a shelf along the back -- one in
     * every wood, so a bench matches the camp it stands in. Keyed by wood id.
     */
    public static final java.util.Map<String, DeferredBlock<TribalBenchBlock>> TRIBAL_BENCHES =
            java.util.Collections.unmodifiableMap(benches());

    private static java.util.Map<String, DeferredBlock<TribalBenchBlock>> benches() {
        java.util.Map<String, DeferredBlock<TribalBenchBlock>> out = new java.util.LinkedHashMap<>();
        for (var wood : tk.darrow.tribalpower.bench.BenchWoods.ALL)
            out.put(wood.id(), BLOCKS.register(wood.block(),
                    () -> new TribalBenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                            .strength(2.5F).sound(SoundType.WOOD).noOcclusion())));
        return out;
    }

    /** Every bench, for the block entity type and for anything that treats them alike. */
    public static net.minecraft.world.level.block.Block[] allBenches() {
        return TRIBAL_BENCHES.values().stream().map(DeferredBlock::get)
                .toArray(net.minecraft.world.level.block.Block[]::new);
    }
    public static final DeferredBlock<LatticeUtilityBlock> SPIRIT_CISTERN = utility("spirit_cistern");

    // The Listening Pit and the Stone Font (design 3.1 sections 6 and 7)
    public static final DeferredBlock<ResonanceMeshBlock> RESONANCE_MESH = BLOCKS.register(
            "resonance_mesh",
            () -> new ResonanceMeshBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(3.5F)
                    .sound(SoundType.COPPER))
    );

    public static final DeferredBlock<Block> ANCHOR_STONE = BLOCKS.registerSimpleBlock(
            "anchor_stone",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(2.5F, 8.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<RitualMarkBlock> RITUAL_MARK = BLOCKS.register(
            "ritual_mark",
            () -> new RitualMarkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.WOOL)
                    .replaceable())
    );

    public static final DeferredBlock<StoneFontBlock> STONE_FONT = BLOCKS.register(
            "stone_font",
            () -> new StoneFontBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5F)
                    .sound(SoundType.STONE))
    );

    public static final DeferredBlock<PulseCairnBlock> PULSE_CAIRN = BLOCKS.register(
            "pulse_cairn",
            () -> new PulseCairnBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(s -> 3))
    );

    public static final DeferredBlock<RitualBrazierBlock> RITUAL_BRAZIER = BLOCKS.register("ritual_brazier", () ->
            new RitualBrazierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3F).sound(SoundType.COPPER).lightLevel(s -> 5).noOcclusion()));

    public static final DeferredBlock<PulseLightBlock> GLOW_REED = BLOCKS.register("glow_reed", () ->
            new PulseLightBlock(PulseLightBlock.Kind.GLOW_REED, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.8F).sound(SoundType.WOOD).noOcclusion()
                    .ignitedByLava()
                    .lightLevel(s -> s.getValue(PulseLightBlock.LIT) ? PulseLightBlock.Kind.GLOW_REED.light : 0)));
    public static final DeferredBlock<PulseLightBlock> SHARD_LAMP = BLOCKS.register("shard_lamp", () ->
            new PulseLightBlock(PulseLightBlock.Kind.SHARD_LAMP, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).strength(1.2F).sound(SoundType.WOOD).noOcclusion()
                    .ignitedByLava()
                    .lightLevel(s -> s.getValue(PulseLightBlock.LIT) ? PulseLightBlock.Kind.SHARD_LAMP.light : 0)));
    public static final DeferredBlock<EchoSconceBlock> ECHO_SCONCE = BLOCKS.register("echo_sconce", () ->
            new EchoSconceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.COPPER).noOcclusion()
                    .lightLevel(s -> s.getValue(PulseLightBlock.LIT) ? PulseLightBlock.Kind.ECHO_SCONCE.light : 0)));
    public static final DeferredBlock<PulseLightBlock> EMBER_BOWL = BLOCKS.register("ember_bowl", () ->
            new PulseLightBlock(PulseLightBlock.Kind.EMBER_BOWL, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).strength(1.5F).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(s -> s.getValue(PulseLightBlock.LIT) ? PulseLightBlock.Kind.EMBER_BOWL.light : 0)));

    public static final DeferredBlock<CampDecorBlock> MARCH_STOOL = BLOCKS.register("march_stool", () ->
            new CampDecorBlock(CampDecorBlock.Kind.STOOL, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN).strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<CampDecorBlock> MARCH_TABLE = BLOCKS.register("march_table", () ->
            new CampDecorBlock(CampDecorBlock.Kind.TABLE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN).strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<CampDecorBlock> SPIRIT_URN = BLOCKS.register("spirit_urn", () ->
            new CampDecorBlock(CampDecorBlock.Kind.URN, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_CYAN).strength(1.8F).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredBlock<WovenMatBlock> WOVEN_MAT = BLOCKS.register("woven_mat", () ->
            new WovenMatBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).strength(0.1F).sound(SoundType.WOOL).noOcclusion()
                    .ignitedByLava().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<WallShelfBlock> WALL_SHELF = BLOCKS.register("wall_shelf", () ->
            new WallShelfBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN).strength(1.2F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<WindCharmBlock> WIND_CHARM = BLOCKS.register("wind_charm", () ->
            new WindCharmBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).instabreak().sound(SoundType.AMETHYST).noOcclusion()
                    .noCollission().pushReaction(PushReaction.DESTROY)));

    public static boolean isMarchTillable(BlockState state) {
        return state.is(MARCH_SOIL.get()) || state.is(MARCH_GRASS.get()) || state.is(MARCH_MOSS.get());
    }

    /** Hoe March soils into March farmland when the block above is air. */
    public static void tillMarchSoil(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.HOE_TILL) return;
        if (!isMarchTillable(event.getState()) && !event.getState().is(MARCH_PATH.get())) return;
        var ctx = event.getContext();
        if (ctx != null && !ctx.getLevel().getBlockState(ctx.getClickedPos().above()).isAir()) return;
        event.setFinalState(MARCH_FARMLAND.get().defaultBlockState());
    }

    /** Shovel March soils into March path when the block above is air. */
    public static void flattenMarchSoil(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.SHOVEL_FLATTEN) return;
        if (!isMarchTillable(event.getState())) return;
        var ctx = event.getContext();
        if (ctx != null && (ctx.getClickedFace() == Direction.DOWN
                || !ctx.getLevel().getBlockState(ctx.getClickedPos().above()).isAir())) return;
        event.setFinalState(MARCH_PATH.get().defaultBlockState());
    }
}
