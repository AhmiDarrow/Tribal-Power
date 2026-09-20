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
        h.assertTrue(shears.getMaxDamage() >= SpiritGear.TOOL_DURABILITY && hoe.getMaxDamage() >= SpiritGear.TOOL_DURABILITY,
                "Spiritgear lasts at least " + SpiritGear.TOOL_DURABILITY);
        h.assertTrue(shears.getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState()) > 1.0F, "Shears cut leaves fast");
        h.assertTrue(hoe.isCorrectToolForDrops(Blocks.HAY_BLOCK.defaultBlockState()), "The hoe takes hay");
        h.assertTrue(SpiritGear.allRankFormulae().stream().anyMatch(f -> f.id().getPath().contains("spiritgear_shears")),
                "Echo stations must offer a rank for the shears");
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
        h.assertTrue(hoe.getDamageValue() == 0, "A paid reap mends rather than wears");
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
}
