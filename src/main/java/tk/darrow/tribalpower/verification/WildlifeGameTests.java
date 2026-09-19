package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.wildlife.Wildlife;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class WildlifeGameTests {
    private static void pool(GameTestHelper h) {
        // Room for a Veil Ray (1.4 blocks wide) to turn without being pushed out: a 5x5 pool, 4 deep, under glass.
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y < 5; y++) h.setBlock(x, y, z, x == 0 || z == 0 || x == 6 || z == 6 ? Blocks.GLASS : Blocks.WATER);
            h.setBlock(x, 5, z, Blocks.GLASS);
        }
    }

    /** Every water creature lives in a pool: it swims, keeps its air, and is not mistaken for a bucketable fish. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void marchWaterLifeLivesInWater(GameTestHelper h) {
        pool(h);
        var types = java.util.List.of(Wildlife.GLIMMERFIN.get(), Wildlife.DRIFT_BELL.get(), Wildlife.VEIL_RAY.get(), Wildlife.SILT_EEL.get());
        var spawned = types.stream().map(type -> (Mob) h.spawn(type, new BlockPos(3, 2, 3))).toList();
        h.runAfterDelay(100, () -> {
            for (Mob mob : spawned) {
                h.assertTrue(mob.isAlive(), mob.getType().getDescriptionId() + " must survive in water");
                h.assertTrue(mob.isInWater(), mob.getType().getDescriptionId() + " must stay in the water");
            }
            h.succeed();
        });
    }

    /** Each March wood's sapling grows its own tree, built from its own logs, with its own leaves. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyMarchSaplingGrowsItsOwnWood(GameTestHelper h) {
        var woods = tk.darrow.tribalpower.world.MarchWoods.Wood.values();
        for (int i = 0; i < woods.length; i++) {
            if (woods[i] == tk.darrow.tribalpower.world.MarchWoods.Wood.STRIDER) continue; // stilts: grown in the survey instead
            var set = tk.darrow.tribalpower.world.MarchWoods.of(woods[i]);
            BlockPos at = new BlockPos(i * 40, 1, 0);
            h.setBlock(at.below(), tk.darrow.tribalpower.block.ModBlocks.MARCH_GRASS.get());
            h.setBlock(at, set.sapling.get());
            var abs = h.absolutePos(at);
            for (int k = 0; k < 2 && h.getLevel().getBlockState(abs).is(set.sapling.get()); k++)
                set.sapling.get().advanceTree(h.getLevel(), abs, h.getLevel().getBlockState(abs), h.getLevel().getRandom());
            h.assertTrue(h.getLevel().getBlockState(abs).is(set.log.get()), woods[i] + " sapling must grow into its own log, got "
                    + h.getLevel().getBlockState(abs));
            boolean leaves = false;
            for (BlockPos pos : BlockPos.betweenClosed(abs.offset(-14, 0, -14), abs.offset(14, 30, 14)))
                if (h.getLevel().getBlockState(pos).is(set.leaves.get())) { leaves = true; break; }
            h.assertTrue(leaves || woods[i] == tk.darrow.tribalpower.world.MarchWoods.Wood.CINDER, woods[i] + " must grow its own leaves");
            // An axe strips the log.
            var event = new net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent(h.getLevel().getBlockState(abs),
                    new net.minecraft.world.item.context.UseOnContext(h.getLevel(), null, InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE),
                            new net.minecraft.world.phys.BlockHitResult(abs.getCenter(), net.minecraft.core.Direction.UP, abs, false)),
                    net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP, false);
            tk.darrow.tribalpower.world.MarchWoods.strip(event);
            h.assertTrue(event.getFinalState().is(set.strippedLog.get()), woods[i] + " log must strip");
        }
        h.succeed();
    }

    /** The March's water and cave decoration can stand where worldgen puts it. */
    @GameTest(template = "empty")
    public static void marchDecorationSurvivesItsPlaces(GameTestHelper h) {
        pool(h);
        var level = h.getLevel();
        h.setBlock(2, 4, 2, Blocks.AIR);
        var surface = h.absolutePos(new BlockPos(2, 4, 2));
        h.assertTrue(tk.darrow.tribalpower.world.MarchDecor.LILY_PAD.get().defaultBlockState().canSurvive(level, surface),
                "A lily pad must sit on still water, below is " + level.getBlockState(surface.below()));
        h.assertTrue(tk.darrow.tribalpower.world.MarchDecor.MOON_LILY.get().defaultBlockState().canSurvive(level, surface), "A moon lily too");
        var bed = h.absolutePos(new BlockPos(2, 1, 2));
        h.assertTrue(tk.darrow.tribalpower.world.MarchDecor.RIBBON_WEED.get().defaultBlockState().canSurvive(level, bed), "Ribbon weed on a lake bed");
        h.succeed();
    }

    /** A water bucket scoops a Glimmerfin, but not the larger water life. */
    @GameTest(template = "empty")
    public static void onlyGlimmerfinGoesInABucket(GameTestHelper h) {
        pool(h);
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        var fish = h.spawn(Wildlife.GLIMMERFIN.get(), new BlockPos(2, 2, 2));
        var eel = h.spawn(Wildlife.SILT_EEL.get(), new BlockPos(2, 2, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        eel.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(eel.isAlive() && player.getMainHandItem().is(Items.WATER_BUCKET), "A Silt Eel must not go in a bucket");
        fish.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().is(Wildlife.GLIMMERFIN_BUCKET.get()) || player.getInventory().contains(new ItemStack(Wildlife.GLIMMERFIN_BUCKET.get())),
                "A water bucket must scoop a Glimmerfin");
        h.succeed();
    }
}
