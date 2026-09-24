package tk.darrow.tribalpower.world;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.PairedDoorBlock;
import tk.darrow.tribalpower.item.ModItems;

/**
 * Each of the March's own trees has its own wood: logs, bark, stripped forms, planks and the full building
 * set, its own leaves, and a sapling that grows it. Assets and data come from tools/generate_march_woods.py.
 */
public final class MarchWoods {
    public enum Wood {
        WILLOW("willow", MapColor.TERRACOTTA_LIGHT_GREEN, MapColor.COLOR_LIGHT_GREEN, "young_willow", "weeping_colossus"),
        HEARTHOAK("hearthoak", MapColor.TERRACOTTA_BROWN, MapColor.COLOR_GREEN, "hearthoak", null),
        BELLCAP("bellcap", MapColor.TERRACOTTA_PURPLE, MapColor.COLOR_PURPLE, "bellcap", null),
        FROSTPINE("frostpine", MapColor.TERRACOTTA_RED, MapColor.COLOR_LIGHT_BLUE, "frostpine", null),
        CINDER("cinder", MapColor.COLOR_BLACK, MapColor.COLOR_ORANGE, "cinder_snag", null),
        STRIDER("strider", MapColor.TERRACOTTA_CYAN, MapColor.COLOR_CYAN, "strider", null);

        public final String id;
        final MapColor wood, foliage;
        final String tree, megaTree;

        Wood(String id, MapColor wood, MapColor foliage, String tree, String megaTree) {
            this.id = id;
            this.wood = wood;
            this.foliage = foliage;
            this.tree = tree;
            this.megaTree = megaTree;
        }
    }

    public static final class Set {
        public DeferredBlock<RotatedPillarBlock> log, wood, strippedLog, strippedWood;
        public DeferredBlock<Block> planks;
        public DeferredBlock<LeavesBlock> leaves;
        public DeferredBlock<SaplingBlock> sapling;
    }

    public static final Map<Wood, Set> SETS = new EnumMap<>(Wood.class);
    /** Every block item, in creative-tab order. */
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();

    static {
        for (Wood wood : Wood.values()) SETS.put(wood, build(wood));
    }

    private MarchWoods() {}

    public static void init() {}

    public static Set of(Wood wood) {
        return SETS.get(wood);
    }

    private static Set build(Wood w) {
        Set set = new Set();
        String id = w.id;
        // Cinderwood grew up in the Ember Wastes: it does not catch fire.
        Supplier<BlockBehaviour.Properties> wood = () -> {
            var p = BlockBehaviour.Properties.of().mapColor(w.wood).strength(2.0F, 3.0F).sound(SoundType.WOOD);
            return w == Wood.CINDER ? p : p.ignitedByLava();
        };
        set.log = block(id + "_log", () -> new RotatedPillarBlock(wood.get()));
        set.wood = block(id + "_wood", () -> new RotatedPillarBlock(wood.get()));
        set.strippedLog = block("stripped_" + id + "_log", () -> new RotatedPillarBlock(wood.get()));
        set.strippedWood = block("stripped_" + id + "_wood", () -> new RotatedPillarBlock(wood.get()));
        set.planks = block(id + "_planks", () -> new Block(wood.get()));
        block(id + "_stairs", () -> new StairBlock(set.planks.get().defaultBlockState(), wood.get()));
        block(id + "_slab", () -> new SlabBlock(wood.get()));
        block(id + "_fence", () -> new FenceBlock(wood.get()));
        block(id + "_fence_gate", () -> new FenceGateBlock(WoodType.OAK, wood.get().forceSolidOn()));
        DeferredBlock<PairedDoorBlock> door = ModBlocks.BLOCKS.register(id + "_door", () -> new PairedDoorBlock(BlockSetType.OAK,
                wood.get().noOcclusion().pushReaction(PushReaction.DESTROY)));
        ITEMS.put(id + "_door", ModItems.ITEMS.register(id + "_door", () -> new DoubleHighBlockItem(door.get(), new Item.Properties())));
        block(id + "_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, wood.get().noOcclusion().isValidSpawn((s, l, p, e) -> false)));
        block(id + "_pressure_plate", () -> new PressurePlateBlock(BlockSetType.OAK, wood.get().forceSolidOn().noCollission()
                .strength(0.5F).pushReaction(PushReaction.DESTROY)));
        block(id + "_button", () -> new ButtonBlock(BlockSetType.OAK, 30, wood.get().noCollission().strength(0.5F)
                .pushReaction(PushReaction.DESTROY)));
        set.leaves = block(id + "_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.MARCH_LEAVES.get())
                .mapColor(w.foliage)));
        TreeGrower grower = new TreeGrower(TribalPower.MOD_ID + ":" + id,
                Optional.ofNullable(w.megaTree).map(MarchWoods::feature), Optional.of(feature(w.tree)), Optional.empty());
        set.sapling = block(id + "_sapling", () -> new SaplingBlock(grower, BlockBehaviour.Properties.ofFullCopy(ModBlocks.MARCH_SAPLING.get())
                .mapColor(w.foliage)));
        return set;
    }

    private static ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> feature(String tree) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "march_" + tree));
    }

    private static <B extends Block> DeferredBlock<B> block(String id, Supplier<B> factory) {
        DeferredBlock<B> block = ModBlocks.BLOCKS.register(id, factory);
        ITEMS.put(id, ModItems.ITEMS.registerSimpleBlockItem(id, block));
        return block;
    }

    /** An axe strips any March log or bark block, as it does vanilla wood. */
    public static void strip(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) return;
        BlockState state = event.getState();
        for (Set set : SETS.values()) {
            RotatedPillarBlock to = state.is(set.log.get()) ? set.strippedLog.get() : state.is(set.wood.get()) ? set.strippedWood.get() : null;
            if (to != null) {
                event.setFinalState(to.defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)));
                return;
            }
        }
    }
}
