package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.material.Fluids;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritFlaskItem;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.storage.InventorySorter;

/** The carried kit added in 3.7: shears, hoe, flask, wrench, wand, and the tidy button behind it. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class ToolsGameTests {

    @GameTest(template = "empty")
    public static void shearsAndHoeAreSpiritgear(GameTestHelper h) {
        ItemStack shears = new ItemStack(ModItems.SPIRITGEAR_SHEARS.get());
        ItemStack hoe = new ItemStack(ModItems.SPIRITGEAR_HOE.get());
        h.assertTrue(SpiritGear.isTool(shears) && SpiritGear.isTool(hoe), "Both rank and bind like Spiritgear");
        h.assertTrue(shears.getMaxDamage() == 1024 && SpiritGear.UNTIERED_DURABILITY == 1024,
                "The shears last 1,024, got " + shears.getMaxDamage());
        h.assertTrue(hoe.getMaxDamage() == 1561, "The hoe lasts 1,561 like the other tiered tools, got " + hoe.getMaxDamage());
        h.assertTrue(shears.getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState()) > 1.0F, "Shears cut leaves fast");
        h.assertTrue(hoe.isCorrectToolForDrops(Blocks.HAY_BLOCK.defaultBlockState()), "The hoe takes hay");
        h.assertTrue(SpiritGear.allRankFormulae().stream().anyMatch(f -> f.id().getPath().contains("spiritgear_shears")),
                "Echo stations must offer a rank for the shears");
        h.succeed();
    }

    /** Every shears-only drop asks for the shearing ability, not the vanilla item, so Spiritgear shears take it too. */
    @GameTest(template = "empty")
    public static void spiritgearShearsTakeShearsOnlyDrops(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        var pos = new BlockPos(1, 2, 1);
        ItemStack spirit = new ItemStack(ModItems.SPIRITGEAR_SHEARS.get());
        h.assertTrue(spirit.is(net.minecraft.tags.ItemTags.MINING_ENCHANTABLE) && spirit.is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE),
                "Spiritgear shears take Efficiency, Unbreaking and Mending like vanilla shears");
        h.assertTrue(net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(spirit.getItem())
                        instanceof net.minecraft.core.dispenser.ShearsDispenseItemBehavior,
                "A dispenser shears with Spiritgear shears like vanilla ones");
        for (String id : new String[] {"ribbon_weed", "veil_lichen", "echo_roots", "willow_strand", "willow_leaves", "march_leaves"}) {
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tribalpower", id));
            for (ItemStack shears : new ItemStack[] {new ItemStack(Items.SHEARS), new ItemStack(ModItems.SPIRITGEAR_SHEARS.get())}) {
                var drops = net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(), h.getLevel(),
                        h.absolutePos(pos), null, player, shears);
                h.assertTrue(drops.stream().anyMatch(s -> s.is(block.asItem())),
                        id + " must drop itself to " + shears.getHoverName().getString() + ", got " + drops);
            }
        }
        h.succeed();
    }

    /** The building stones and their shapes all have a recipe, and the March sands work like vanilla sand. */
    @GameTest(template = "empty")
    public static void marchBuildingBlocksAreCraftable(GameTestHelper h) {
        var level = h.getLevel();
        var made = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (var recipe : level.getRecipeManager().getRecipes())
            made.add(recipe.value().getResultItem(level.registryAccess()).getItem());
        var ids = new java.util.ArrayList<>(java.util.List.of("moonstone_bricks", "polished_moonstone", "moss_agate_bricks",
                "polished_moss_agate", "ochre_sandstone", "salt_pillar"));
        for (String base : new String[] {"pale_stone", "frost_shale", "ochre_sandstone", "salt_crust",
                "kiln_clay_ash", "kiln_clay_bone", "kiln_clay_ochre", "kiln_clay_rust"})
            for (String shape : new String[] {"_stairs", "_slab", "_wall"}) ids.add(base + shape);
        tk.darrow.tribalpower.world.MarchBuilding.ITEMS.keySet().forEach(ids::add);
        for (String id : ids) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tribalpower", id));
            h.assertTrue(item != Items.AIR, id + " must be registered");
            h.assertTrue(made.contains(item), id + " must have a recipe");
        }
        for (String sand : new String[] {"pale_sand", "ochre_sand", "glass_sand"}) {
            var stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tribalpower", sand)));
            var glass = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level);
            h.assertTrue(glass.isPresent() && glass.get().value().getResultItem(level.registryAccess()).is(Items.GLASS),
                    sand + " must smelt to glass like vanilla sand");
            h.assertTrue(stack.is(net.neoforged.neoforge.common.Tags.Items.SANDS), sand + " must be c:sands");
        }
        h.assertTrue(made.contains(Items.TNT) && made.contains(Items.WHITE_CONCRETE_POWDER), "Sand recipes must load");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hoeReapsAndSowsRipeWheat(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        ItemStack hoe = new ItemStack(ModItems.SPIRITGEAR_HOE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        player.getInventory().setItem(1, PulseCellItem.createFilled(200));
        BlockPos soil = new BlockPos(2, 1, 2), crop = soil.above();
        h.setBlock(soil, Blocks.FARMLAND);
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        h.setBlock(crop, wheat.defaultBlockState().setValue(CropBlock.AGE, 7));
        BlockPos absolute = h.absolutePos(crop);
        hoe.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, hoe,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        BlockState after = h.getLevel().getBlockState(absolute);
        h.assertTrue(after.is(Blocks.WHEAT) && after.getValue(CropBlock.AGE) == 0,
                "The crop is sown again at age 0, not broken");
        h.assertTrue(hoe.getDamageValue() == 0, "A paid reap takes no wear");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void flaskCarriesAndPoursFluid(GameTestHelper h) {
        ItemStack flask = new ItemStack(ModItems.SPIRIT_FLASK.get());
        IFluidHandler handler = SpiritFlaskItem.handler(flask);
        h.assertTrue(handler.getTanks() > 0, "The flask is a fluid container");
        int filled = handler.fill(new FluidStack(Fluids.WATER, 3 * FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled == 3 * FluidType.BUCKET_VOLUME, "Three buckets go in, got " + filled);
        ItemStack held = ((net.neoforged.neoforge.fluids.capability.IFluidHandlerItem) handler).getContainer();
        h.assertTrue(SpiritFlaskItem.contents(held).getAmount() == 3 * FluidType.BUCKET_VOLUME,
                "The flask remembers what it carries");
        int over = SpiritFlaskItem.handler(held).fill(new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME),
                IFluidHandler.FluidAction.SIMULATE);
        h.assertTrue(over == 0, "One fluid at a time");
        h.assertTrue(((SpiritFlaskItem) ModItems.GREATER_SPIRIT_FLASK.get()).capacity()
                > ((SpiritFlaskItem) ModItems.SPIRIT_FLASK.get()).capacity(), "The greater flask holds more");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wrenchTurnsAMachineFace(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.setGameMode(GameType.SURVIVAL);
        ItemStack wrench = new ItemStack(ModItems.TOTEM_WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, Blocks.FURNACE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        BlockPos absolute = h.absolutePos(pos);
        wrench.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, wrench,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        Direction now = h.getLevel().getBlockState(absolute)
                .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        h.assertTrue(now != Direction.NORTH, "The wrench turns a block that has a facing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wandLaysAFaceOfBlocks(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        ItemStack wand = new ItemStack(ModItems.WEAVERS_WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 16));
        player.getInventory().setItem(2, PulseCellItem.createFilled(400));
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        BlockPos clicked = h.absolutePos(new BlockPos(2, 1, 2));
        wand.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, wand,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
        int laid = 0;
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++)
            if (h.getBlockState(new BlockPos(x, 2, z)).is(Blocks.STONE)) laid++;
        h.assertTrue(laid == 9, "The wand carries the whole face upward, laid " + laid);
        h.assertTrue(player.getInventory().getItem(1).getCount() == 7, "Nine of the sixteen stone are spent");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wandSpendsALaterStackAfterTheFirstRunsOut(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        ItemStack wand = new ItemStack(ModItems.WEAVERS_WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 1));
        player.getInventory().setItem(2, new ItemStack(Items.STONE, 8));
        player.getInventory().setItem(3, PulseCellItem.createFilled(400));
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        BlockPos clicked = h.absolutePos(new BlockPos(2, 1, 2));
        wand.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, wand,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
        int left = player.getInventory().countItem(Items.STONE);
        h.assertTrue(left == 0, "Both stacks are spent, " + left + " stone remained");
        h.assertTrue(!player.getInventory().getItem(1).is(Items.STONE), "The emptied slot is clear");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tidyMergesAndOrdersAChest(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.STONE, 12));
        chest.setItem(5, new ItemStack(Items.APPLE, 3));
        chest.setItem(9, new ItemStack(Items.STONE, 30));
        player.containerMenu = new ChestMenu(MenuType.GENERIC_9x3, 1, player.getInventory(), chest, 3);
        h.assertTrue(InventorySorter.sort(player, true), "A chest with loose stacks can be tidied");
        h.assertTrue(chest.getItem(0).is(Items.APPLE), "Apples sort before stone");
        h.assertTrue(chest.getItem(1).is(Items.STONE) && chest.getItem(1).getCount() == 42,
                "Two stone stacks merge into one of 42, got " + chest.getItem(1).getCount());
        h.assertTrue(chest.getItem(2).isEmpty() && chest.getItem(9).isEmpty(), "Everything else is left empty");
        h.assertTrue(!InventorySorter.sort(player, true), "A tidy chest is left alone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tidyRefusesMachineSlots(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.ECHO_SHATTER.get());
        var be = h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(be instanceof net.minecraft.world.MenuProvider, "The station opens a menu");
        var menu = ((net.minecraft.world.MenuProvider) be).createMenu(1, player.getInventory(), player);
        player.containerMenu = menu;
        h.assertTrue(InventorySorter.group(menu, player, true).isEmpty(),
                "A station's input, catalyst and output slots are never rearranged");
        h.assertTrue(!InventorySorter.group(menu, player, false).isEmpty(),
                "The player's own pack is still tidyable while a station is open");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tidySortsTheDeepCacheVault(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        var vault = tk.darrow.tribalpower.storage.DeepCacheManager.openContainer(h.getLevel().getServer(), player.getUUID(), true);
        vault.clearContent();
        vault.setItem(0, new ItemStack(Items.STONE, 12));
        vault.setItem(7, new ItemStack(Items.APPLE, 3));
        vault.setItem(40, new ItemStack(Items.STONE, 30));
        // The same menu the Deep Cache block and the Wayfarer Satchel open.
        var menu = tk.darrow.tribalpower.storage.DeepCacheContainer.guard(ChestMenu.sixRows(1, player.getInventory(), vault), vault);
        player.containerMenu = menu;
        h.assertTrue(menu.stillValid(player), "The owner's vault menu is valid");
        h.assertTrue(!InventorySorter.group(menu, player, true).isEmpty(), "The vault side has a tidy button");
        h.assertTrue(InventorySorter.sort(player, true), "A vault with loose stacks can be tidied");
        h.assertTrue(vault.getItem(0).is(Items.APPLE), "Apples sort before stone");
        h.assertTrue(vault.getItem(1).is(Items.STONE) && vault.getItem(1).getCount() == 42,
                "Two stone stacks merge into one of 42, got " + vault.getItem(1).getCount());
        h.assertTrue(vault.getItem(7).isEmpty() && vault.getItem(40).isEmpty(), "Everything else is left empty");
        h.assertTrue(!menu.slots.get(0).mayPlace(new ItemStack(ModItems.WAYFARER_SATCHEL.get())),
                "The vault still refuses its own key");
        vault.clearContent();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tidySortsAnAncestralCache(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.ANCESTRAL_CACHE.get());
        player.moveTo(Vec3.atCenterOf(h.absolutePos(pos.east())));
        var be = h.getLevel().getBlockEntity(h.absolutePos(pos));
        h.assertTrue(be instanceof tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity, "The cache has its block entity");
        var cache = (tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity) be;
        cache.setItem(3, new ItemStack(Items.STONE, 12));
        cache.setItem(20, new ItemStack(Items.APPLE, 3));
        cache.setItem(53, new ItemStack(Items.STONE, 30));
        var menu = cache.createMenu(1, player.getInventory(), player);
        h.assertTrue(menu != null, "The cache opens a menu");
        player.containerMenu = menu;
        h.assertTrue(menu.stillValid(player), "The cache menu is valid beside the cache");
        h.assertTrue(!InventorySorter.group(menu, player, true).isEmpty(), "The cache side has a tidy button");
        h.assertTrue(InventorySorter.sort(player, true), "A cache with loose stacks can be tidied");
        h.assertTrue(cache.getItem(0).is(Items.APPLE), "Apples sort before stone");
        h.assertTrue(cache.getItem(1).is(Items.STONE) && cache.getItem(1).getCount() == 42,
                "Two stone stacks merge into one of 42, got " + cache.getItem(1).getCount());
        h.assertTrue(cache.getItem(3).isEmpty() && cache.getItem(53).isEmpty(), "Everything else is left empty");
        h.succeed();
    }
}
