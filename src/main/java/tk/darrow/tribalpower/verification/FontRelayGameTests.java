package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.RelayBlock;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.SpiritCisternBlockEntity;
import tk.darrow.tribalpower.blockentity.StoneFontBlockEntity;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;
import tk.darrow.tribalpower.lattice.SideIo;

/** The Stone Font's top and bottom each take an item plate (stone out) and a fluid plate (water and lava in). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class FontRelayGameTests {
    private static <T> T at(GameTestHelper h, BlockPos pos, Class<T> type) { return type.cast(h.getLevel().getBlockEntity(h.absolutePos(pos))); }

    @GameTest(template = "empty")
    public static void fontTopAndBottomTakeItemsOutAndFluidIn(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2);
        h.setBlock(rel, ModBlocks.STONE_FONT.get());
        var font = at(h, rel, StoneFontBlockEntity.class);
        var abs = h.absolutePos(rel);
        for (Direction face : new Direction[]{Direction.UP, Direction.DOWN}) {
            var fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, face);
            h.assertTrue(fluids != null && fluids.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.SIMULATE) == 250
                    && fluids.fill(new FluidStack(Fluids.LAVA, 250), IFluidHandler.FluidAction.SIMULATE) == 250,
                    "The font's " + face + " face takes water and lava");
            font.water.setFluid(new FluidStack(Fluids.WATER, 1000));
            h.assertTrue(fluids.drain(250, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "But never gives its tanks back through " + face);
            font.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
            var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, face);
            boolean gives = false;
            for (int slot = 0; items != null && slot < items.getSlots(); slot++)
                if (!items.extractItem(slot, 1, true).isEmpty()) gives = true;
            h.assertTrue(gives, "An item plate on the font's " + face + " face draws the stone out");
            h.assertTrue(items.insertItem(0, new ItemStack(Items.COBBLESTONE), true).getCount() == 1, "And nothing goes in through " + face);
        }
        for (Direction side : Direction.Plane.HORIZONTAL)
            h.assertTrue(font.sideIo().get(side) == SideIo.Mode.INPUT, "The sides stay input, " + side);
        // A font saved with the old station faces (input top, output bottom) loads with the new ones.
        var tag = font.saveWithFullMetadata(h.getLevel().registryAccess());
        tag.putInt("SideIo", SideIo.station().pack());
        font.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(font.sideIo().pack() == StoneFontBlockEntity.fontSides().pack(), "An old untouched font takes the new faces");
        // One a player set by hand keeps it.
        font.sideIo().set(Direction.UP, SideIo.Mode.NONE);
        tag = font.saveWithFullMetadata(h.getLevel().registryAccess());
        font.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(font.sideIo().get(Direction.UP) == SideIo.Mode.NONE, "A face set by hand survives");
        h.succeed();
    }

    /**
     * The way it was reported broken: an item plate sat on the font's top found nothing to take, and a fluid plate tuned
     * to the font's bottom was turned away. Both plates now work on the one font.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void itemPlateOnTopAndFluidPlateIntoTheBottom(GameTestHelper h) {
        BlockPos fontPos = new BlockPos(2, 2, 2);
        h.setBlock(fontPos, ModBlocks.STONE_FONT.get());
        h.setBlock(fontPos.above(), ModBlocks.ITEM_RELAY.get().defaultBlockState().setValue(RelayBlock.FACING, Direction.UP));
        h.setBlock(new BlockPos(6, 2, 2), Blocks.CHEST);
        h.setBlock(new BlockPos(5, 1, 5), ModBlocks.SPIRIT_CISTERN.get());
        h.setBlock(new BlockPos(5, 2, 5), ModBlocks.FLUID_RELAY.get().defaultBlockState().setValue(RelayBlock.FACING, Direction.UP));
        h.setBlock(new BlockPos(0, 2, 0), ModBlocks.RESONANCE_TOTEM_AIR.get());
        h.setBlock(new BlockPos(3, 2, 4), ModBlocks.DRUMHEART.get());
        at(h, new BlockPos(3, 2, 4), DrumheartBlockEntity.class).insertPulse(1000, false);
        var font = at(h, fontPos, StoneFontBlockEntity.class);
        font.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        var chest = at(h, new BlockPos(6, 2, 2), ChestBlockEntity.class);
        var cistern = at(h, new BlockPos(5, 1, 5), SpiritCisternBlockEntity.class);
        cistern.tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        String dim = h.getLevel().dimension().location().toString();
        var itemPlate = at(h, fontPos.above(), WirelessRelayBlockEntity.class);
        var fluidPlate = at(h, new BlockPos(5, 2, 5), WirelessRelayBlockEntity.class);
        h.assertTrue(itemPlate.bind(h.absolutePos(new BlockPos(6, 2, 2)), Direction.UP, dim), "The item plate tunes to the chest");
        h.assertTrue(fluidPlate.bind(h.absolutePos(fontPos), Direction.DOWN, dim), "The fluid plate tunes to the font's bottom");
        h.runAfterDelay(150, () -> {
            h.assertTrue(chest.countItem(Items.COBBLESTONE) > 0, "The plate on the font's top carries the stone away, chest="
                    + chest.countItem(Items.COBBLESTONE) + ", plate=" + itemPlate.status().getString());
            h.assertTrue(font.water.getFluidAmount() > 0, "The font's bottom takes the water, font=" + font.water.getFluidAmount()
                    + ", plate=" + fluidPlate.status().getString());
            h.succeed();
        });
    }
}
