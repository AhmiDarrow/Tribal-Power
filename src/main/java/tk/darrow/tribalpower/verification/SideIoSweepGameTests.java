package tk.darrow.tribalpower.verification;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.lattice.HasSideIo;
import tk.darrow.tribalpower.lattice.SideIo;

/**
 * Every Tribal Power block with configurable faces, set the way the I/O pad, a sneak-click or the Totem Wrench sets
 * it ({@link HasSideIo#cycle}): each mode must stick, survive a save, and be what hoppers and pipes actually get.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class SideIoSweepGameTests {
    private static final Direction FACE = Direction.EAST;

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everyFaceSettingDoesWhatItSays(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        var level = h.getLevel();
        BlockPos rel = new BlockPos(2, 2, 2);
        BlockPos pos = h.absolutePos(rel);
        List<String> covered = new ArrayList<>();
        List<String> faults = new ArrayList<>();
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!TribalPower.MOD_ID.equals(id.getNamespace())) continue;
            h.setBlock(rel, Blocks.AIR);
            try {
                h.setBlock(rel, block);
            } catch (RuntimeException cannot) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof HasSideIo io)) continue;
            covered.add(id.getPath());
            for (SideIo.Mode want : new SideIo.Mode[]{SideIo.Mode.INPUT, SideIo.Mode.OUTPUT, SideIo.Mode.NONE, SideIo.Mode.BOTH}) {
                for (int n = 0; n < 4 && io.sideIo().get(FACE) != want; n++) {
                    if (!HasSideIo.cycle(player, be, FACE)) { faults.add(id.getPath() + ": refused a cycle"); break; }
                }
                if (io.sideIo().get(FACE) != want) { faults.add(id.getPath() + ": never reached " + want); continue; }
                check(h, id.getPath(), be, io, want, faults);
            }
            // The setting is saved with the block.
            io.sideIo().set(FACE, SideIo.Mode.OUTPUT);
            var saved = be.saveWithFullMetadata(level.registryAccess());
            io.sideIo().set(FACE, SideIo.Mode.BOTH);
            be.loadWithComponents(saved, level.registryAccess());
            if (io.sideIo().get(FACE) != SideIo.Mode.OUTPUT) faults.add(id.getPath() + ": the face setting did not survive a save");
            // The two screens with a pad show the server's setting.
            var menu = be instanceof net.minecraft.world.MenuProvider provider ? provider.createMenu(1, player.getInventory(), player) : null;
            int shown = menu instanceof tk.darrow.tribalpower.echo.CacheMenu cache ? cache.ioPacked()
                    : menu instanceof tk.darrow.tribalpower.echo.StationMenu station ? station.ioPacked() : io.sideIo().pack();
            if (shown != io.sideIo().pack()) faults.add(id.getPath() + ": the pad shows " + shown + " but the block holds " + io.sideIo().pack());
        }
        TribalPower.LOGGER.info("SideIo sweep covered {}: {}", covered.size(), covered);
        h.assertTrue(covered.size() >= 8, "The sweep found the side-IO machines: " + covered);
        h.assertTrue(faults.isEmpty(), String.join("; ", faults));
        h.setBlock(rel, Blocks.AIR);
        h.succeed();
    }

    /** A Resonance Totem is two blocks: the top half can be clicked and broken, and an old one-block totem regrows it. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void totemTopHalfIsReal(GameTestHelper h) {
        var level = h.getLevel();
        var block = tk.darrow.tribalpower.block.ModBlocks.RESONANCE_TOTEM_AIR.get();
        h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        var player = VerificationPlayers.inLevel(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(block));
        BlockPos ground = h.absolutePos(new BlockPos(2, 1, 2));
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false));
        BlockPos lower = ground.above(), upper = lower.above();
        h.assertTrue(level.getBlockState(lower).is(block) && !tk.darrow.tribalpower.block.ResonanceTotemBlock.isUpper(level.getBlockState(lower)), "Placing a totem fills its lower block");
        h.assertTrue(tk.darrow.tribalpower.block.ResonanceTotemBlock.isUpper(level.getBlockState(upper)), "And its upper block");
        h.assertTrue(level.getBlockEntity(upper) == null && level.getBlockEntity(lower) != null, "Only the lower half holds the totem");
        h.assertFalse(level.getBlockState(upper).getShape(level, upper).isEmpty(), "The top half has a hitbox of its own");
        h.assertTrue(tk.darrow.tribalpower.block.ResonanceTotemBlock.base(level, upper).equals(lower), "Uses of the top reach the totem");
        // The mock player always counts as creative, where a totem rightly drops nothing; a fake player is survival.
        net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level).gameMode.destroyBlock(upper);
        h.assertTrue(level.getBlockState(lower).isAir() && level.getBlockState(upper).isAir(), "Breaking the top breaks the whole totem");
        int dropped = 0;
        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, h.getBounds().inflate(3)))
            if (item.getItem().is(block.asItem())) dropped += item.getItem().getCount();
        h.assertTrue(dropped == 1, "And it drops once, dropped=" + dropped);
        // An old totem was one block; it grows its top once there is air above.
        h.setBlock(new BlockPos(2, 2, 2), block);
        h.succeedWhen(() -> h.assertTrue(tk.darrow.tribalpower.block.ResonanceTotemBlock.isUpper(level.getBlockState(upper)), "An old totem regrows its top"));
    }

    private static void check(GameTestHelper h, String name, BlockEntity be, HasSideIo io, SideIo.Mode mode, List<String> faults) {
        if (be instanceof WorldlyContainer box) {
            int[] slots = box.getSlotsForFace(FACE);
            if (mode == SideIo.Mode.NONE && slots.length != 0) faults.add(name + ": a None face still offers " + slots.length + " slots");
            ItemStack[] samples = {new ItemStack(Items.STONE), new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.WATER_BUCKET)};
            for (int slot = 0; slot < box.getContainerSize(); slot++) {
                for (ItemStack sample : samples) {
                    if (!mode.insert() && box.canPlaceItemThroughFace(slot, sample, FACE))
                        faults.add(name + ": a " + mode + " face accepts " + sample.getItem() + " into slot " + slot);
                    if (!mode.extract() && box.canTakeItemThroughFace(slot, sample, FACE))
                        faults.add(name + ": a " + mode + " face gives up slot " + slot);
                }
            }
        }
        var cap = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), FACE);
        if (cap != null) {
            for (int slot = 0; slot < cap.getSlots(); slot++) {
                if (!mode.insert()) {
                    ItemStack offered = new ItemStack(Items.STONE);
                    if (cap.insertItem(slot, offered.copy(), true).getCount() != offered.getCount())
                        faults.add(name + ": a pipe can insert through a " + mode + " face");
                }
                if (!mode.extract() && !cap.getStackInSlot(slot).isEmpty() && !cap.extractItem(slot, 1, true).isEmpty())
                    faults.add(name + ": a pipe can extract through a " + mode + " face");
            }
        }
        var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), FACE);
        if (fluid != null && !mode.insert()) {
            var water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
            if (fluid.fill(water, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) > 0)
                faults.add(name + ": a pipe can fill through a " + mode + " face");
        }
    }
}
