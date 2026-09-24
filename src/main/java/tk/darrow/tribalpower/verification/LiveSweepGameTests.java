package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity;
import tk.darrow.tribalpower.blockentity.RitualBrazierBlockEntity;
import tk.darrow.tribalpower.blockentity.SongBenchBlockEntity;
import tk.darrow.tribalpower.camp.BoundEffigyItem;
import tk.darrow.tribalpower.camp.CampRegistry;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.cuisine.Dish;
import tk.darrow.tribalpower.cuisine.HearthPotBlockEntity;
import tk.darrow.tribalpower.cuisine.MarchCrop;
import tk.darrow.tribalpower.event.MarchEventsSavedData;
import tk.darrow.tribalpower.guardian.GuardianAltarBlockEntity;
import tk.darrow.tribalpower.healing.Remedies;
import tk.darrow.tribalpower.healing.Remedy;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.rite.world.WorldRite;
import tk.darrow.tribalpower.rite.world.WorldRiteRegistry;
import tk.darrow.tribalpower.song.SongCast;
import tk.darrow.tribalpower.song.SongVerse;

import java.util.List;
import java.util.UUID;

/** What the live sweep turned up: each of these failed in play before its fix. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class LiveSweepGameTests {
    private LiveSweepGameTests() {}

    @GameTest(template = "empty")
    public static void seatedChalkStaysInTheSongBenchAcrossATick(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.SONG_BENCH.get());
        var bench = (SongBenchBlockEntity) h.getBlockEntity(pos);
        bench.setItem(SongBenchBlockEntity.CHALK, new ItemStack(ModItems.RITUAL_CHALK.get()));
        bench.setItem(SongBenchBlockEntity.PAPER, new ItemStack(Items.PAPER, 3));
        SongBenchBlockEntity.serverTick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos), bench);
        SongBenchBlockEntity.serverTick(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos), bench);
        h.assertTrue(bench.getItem(SongBenchBlockEntity.CHALK).is(ModItems.RITUAL_CHALK.get()), "The chalk stays seated");
        h.assertTrue(bench.getItem(SongBenchBlockEntity.PAPER).getCount() == 3, "The paper stays seated");
        h.assertFalse(bench.canPlaceItem(SongBenchBlockEntity.CHALK, new ItemStack(ModItems.RITUAL_CHALK.get())), "A second stick may not join it");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theHearthPotCountsAStackInOneSeat(GameTestHelper h) {
        h.setBlock(2, 2, 2, Blocks.CAMPFIRE);
        h.setBlock(2, 3, 2, CuisineRegistry.HEARTH_POT.get());
        var pot = (HearthPotBlockEntity) h.getBlockEntity(new BlockPos(2, 3, 2));
        var level = (ServerLevel) h.getLevel();
        pot.setItem(0, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.EMBERROOT).get(), 2));
        pot.setItem(1, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.STEPPE_GRAIN).get()));
        pot.setItem(HearthPotBlockEntity.CONTAINER, new ItemStack(Items.BOWL));
        h.assertTrue(pot.cookNow(level), "Two emberroot in one seat and a grain make the mash, state " + pot.state());
        h.assertTrue(pot.getItem(HearthPotBlockEntity.OUTPUT).is(CuisineRegistry.DISHES.get(Dish.KEEPERS_ROOT_MASH).get()), "It is the mash");
        h.assertTrue(pot.getItem(0).isEmpty() && pot.getItem(1).isEmpty(), "Both emberroot and the grain were spent");
        pot.setItem(HearthPotBlockEntity.OUTPUT, ItemStack.EMPTY);
        pot.setItem(0, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.EMBERROOT).get(), 2));
        pot.setItem(1, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.STEPPE_GRAIN).get()));
        pot.setItem(2, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.FROSTBERRY).get()));
        pot.setItem(HearthPotBlockEntity.CONTAINER, new ItemStack(Items.BOWL));
        h.assertFalse(pot.cookNow(level), "A seat nothing asks for spoils the pot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aFeastAndTheEldersGiftAreRememberedApartOnAnOddDay(GameTestHelper h) {
        var data = MarchEventsSavedData.get(h.getLevel().getServer());
        UUID who = UUID.randomUUID();
        int tribe = 4;
        long day = 13;
        h.assertTrue(data.join(who, tribe, day), "The Elder's gift on day 13");
        h.assertTrue(data.join(who, tribe + 16, day), "The feast at the table the same day is its own record");
        h.assertFalse(data.join(who, tribe + 16, day), "And only once");
        h.assertTrue(data.joined(who, tribe, day) && data.joined(who, tribe + 16, day), "Both are remembered");
        h.assertFalse(data.joined(who, tribe, day + 1), "Tomorrow is not remembered yet");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aBrazierStruckByItsOwnSignalFiresTheRite(GameTestHelper h) {
        BlockPos brazierPos = new BlockPos(3, 2, 3);
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) h.setBlock(x, 1, z, Blocks.STONE);
        for (int dx : new int[] {-2, 2}) for (int dz : new int[] {-2, 2}) h.setBlock(brazierPos.offset(dx, 0, dz), ModBlocks.RITE_PEDESTAL.get());
        for (int dx : new int[] {-1, 1}) for (int dz : new int[] {-1, 1}) h.setBlock(brazierPos.offset(dx, 0, dz), ModBlocks.RITUAL_MARK.get());
        h.setBlock(brazierPos, ModBlocks.RITUAL_BRAZIER.get());
        h.setBlock(5, 2, 3, ModBlocks.DRUMHEART.get());
        var brazier = (RitualBrazierBlockEntity) h.getBlockEntity(brazierPos);
        brazier.setSeal(new ItemStack(ModItems.WATER_SEAL.get()));
        var drum = (DrumheartBlockEntity) h.getBlockEntity(new BlockPos(5, 2, 3));
        drum.insertPulse(1000, false);
        var seat = (RitePedestalBlockEntity) h.getBlockEntity(brazierPos.offset(2, 0, 2));
        seat.setItem(RitePedestalBlockEntity.SLOT, new ItemStack(WorldRiteRegistry.TABLETS.get(WorldRite.RAIN_CALLING).get()));
        // the rising signal strikes the brazier at once, and is still high when the rite is asked for
        h.setBlock(brazierPos.above(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(h.getLevel().hasNeighborSignal(h.absolutePos(brazierPos)), "The brazier is under its striking signal");
        h.assertTrue(seat.held().isEmpty(), "The strike under its own signal fires the rite and spends the tablet");
        h.assertTrue(drum.getPulseStored() < 1000, "And the rite cost Pulse");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aSongsDebuffRidersNeverLandOnTheSinger(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.clearFire();
            var ward = new SongVerse(List.of("dawn_velvet", "dawn_velvet", "ember_heart"), Attunement.EARTH);
            h.assertTrue(SongCast.play(player, ward), "A ward with an ember rider plays");
            h.assertTrue(player.getRemainingFireTicks() <= 0, "The ember rider does not set the singer alight");
            h.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "The earth ward still holds");
            var step = new SongVerse(List.of("storm_wing", "storm_wing", "dawn_velvet"), Attunement.AIR);
            h.assertTrue(SongCast.play(player, step), "A step with a quiet rider plays");
            h.assertTrue(player.hasEffect(MobEffects.REGENERATION), "The quiet rider is the singer's own and still plays");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theEarthVoiceGivesARemedyRealAbsorption(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            Remedies.give(player, Remedy.ANTIDOTE, Attunement.EARTH, 10, 0, false);
            h.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "The absorption comes as the effect");
            h.assertTrue(player.getAbsorptionAmount() >= 4, "And it holds four points, got " + player.getAbsorptionAmount());
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aGroveTenderWatersItsBedWhenAWaterTotemKeepsNearby(GameTestHelper h) {
        var level = (ServerLevel) h.getLevel();
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) h.setBlock(x, 1, z, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 0));
        h.setBlock(4, 1, 4, Blocks.STONE);
        h.setBlock(4, 2, 4, tk.darrow.tribalpower.camp.CampRegistry.DEVICES.get("grove_tender").get());
        h.setBlock(4, 5, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var tender = (tk.darrow.tribalpower.camp.CampBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        tender.setOwner(UUID.randomUUID());
        tender.pulse = 200;
        tender.work(level);
        h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) == 0, "Without a Water totem the bed stays dry");
        h.setBlock(6, 2, 7, ModBlocks.RESONANCE_TOTEM_WATER.get());
        ((tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity) h.getBlockEntity(new BlockPos(6, 2, 7))).setAttention(600);
        tender.work(level);
        h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) == 7
                && h.getBlockState(new BlockPos(7, 1, 7)).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) == 7, "A kept Water totem lets the tender soak every furrow");
        h.assertTrue(tender.pulse == 200 - tk.darrow.tribalpower.config.TribalConfig.groveWaterCost(), "The watering costs its Pulse, got " + tender.pulse);
        int after = tender.pulse;
        tender.work(level);
        h.assertTrue(tender.pulse == after, "A wet bed costs nothing more");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smallThingsTheSweepCaught(GameTestHelper h) {
        var unbound = BoundEffigyItem.targetName(new ItemStack(CampRegistry.EFFIGY.get()));
        h.assertTrue(unbound.getContents() instanceof TranslatableContents t && t.getKey().equals("message.tribalpower.effigy.unbound"),
                "An unbound effigy is unbound, not a pig: " + unbound.getString());
        var bread = new ItemStack(CuisineRegistry.DISHES.get(Dish.GRIT_BAKED_BREAD).get());
        var mash = new ItemStack(CuisineRegistry.DISHES.get(Dish.KEEPERS_ROOT_MASH).get());
        h.assertTrue(bread.get(net.minecraft.core.component.DataComponents.FOOD).usingConvertsTo().isEmpty(), "Bread hands back no bowl");
        h.assertTrue(mash.get(net.minecraft.core.component.DataComponents.FOOD).usingConvertsTo().isPresent(), "Mash comes in a bowl");
        h.assertFalse(GuardianAltarBlockEntity.litCandle(Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, false)), "An unlit candle does not dress an altar");
        h.assertTrue(GuardianAltarBlockEntity.litCandle(Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true)), "A lit one does");
        h.assertTrue(tk.darrow.tribalpower.song.Reagents.of(Items.STICK) == null, "A stick is no reagent");
        h.succeed();
    }

    /** A flask clicked on the cistern fills from it: a survival hand drains the tank, a creative hand leaves it full. */
    @GameTest(template = "empty")
    public static void aFlaskClickedOnTheCisternFills(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, tk.darrow.tribalpower.block.ModBlocks.SPIRIT_CISTERN.get());
        var tank = ((tk.darrow.tribalpower.blockentity.SpiritCisternBlockEntity) h.getBlockEntity(pos)).tank;
        tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);   // room for both hands: a flask takes four buckets
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)), net.minecraft.core.Direction.NORTH, h.absolutePos(pos), false);
        for (boolean creative : new boolean[] {false, true}) {
            player.getAbilities().instabuild = creative;
            ItemStack flask = new ItemStack(tk.darrow.tribalpower.item.ModItems.SPIRIT_FLASK.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, flask);
            int before = tank.getFluidAmount();
            var result = h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(flask, h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            h.assertTrue(result.consumesAction(), "The click is taken (" + (creative ? "creative" : "survival") + "): " + result);
            var held = player.getMainHandItem();
            h.assertTrue(tk.darrow.tribalpower.item.SpiritFlaskItem.contents(held).getAmount() > 0, "The flask in hand holds water (" + (creative ? "creative" : "survival") + "): " + held);
            if (creative) h.assertTrue(tank.getFluidAmount() == before, "A creative hand leaves the tank as it was");
            else h.assertTrue(tank.getFluidAmount() < before, "A survival hand drains the tank by what it took");
        }
        h.succeed();
    }
}
