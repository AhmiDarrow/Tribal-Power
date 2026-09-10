package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.PulseResonatorBlockEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.tribe.KinRole;
import tk.darrow.tribalpower.tribe.KinshipTotemBlock;
import tk.darrow.tribalpower.tribe.TribalKinEntity;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeHearthBlock;
import tk.darrow.tribalpower.tribe.TribeHearthBlockEntity;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeRegistry;
import tk.darrow.tribalpower.tribe.TribeStanding;
import tk.darrow.tribalpower.tribe.TribeStandingSavedData;

import java.util.UUID;

/** The Nine Tribes (design 3.0 §2): ranks, hearth offerings, Kinship voices and Elder offers. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class TribeGameTests {
    @GameTest(template = "empty")
    public static void rankThresholds(GameTestHelper h) {
        h.assertTrue(TribeRank.of(0) == TribeRank.STRANGER && TribeRank.of(49) == TribeRank.STRANGER, "0-49 is Stranger");
        h.assertTrue(TribeRank.of(50) == TribeRank.GUEST && TribeRank.of(149) == TribeRank.GUEST, "50-149 is Guest");
        h.assertTrue(TribeRank.of(150) == TribeRank.FRIEND && TribeRank.of(399) == TribeRank.FRIEND, "150-399 is Friend");
        h.assertTrue(TribeRank.of(400) == TribeRank.KIN && TribeRank.of(799) == TribeRank.KIN, "400-799 is Kin");
        h.assertTrue(TribeRank.of(800) == TribeRank.VOICE && TribeRank.of(5000) == TribeRank.VOICE, "800+ is Voice");
        h.assertTrue(TribeDefinition.values().length == 9 && TribeDefinition.SPINDLE.ordinal() == 8
                && TribeDefinition.SPINDLE.attunement() == Attunement.LOOM, "Nine tribes in wire order, spindle last with the Loom voice");
        var data = TribeStandingSavedData.get(h.getLevel().getServer());
        UUID id = UUID.randomUUID();
        data.set(id, TribeDefinition.CLAW, -10);
        h.assertTrue(data.get(id, TribeDefinition.CLAW) == 0, "Standing never drops below zero");
        long day = 7;
        int allowed = 0;
        for (int i = 0; i < 25; i++) if (data.tryKillGain(id, TribeDefinition.SOIL, day, TribeStanding.KILL_CAP_PER_DAY)) allowed++;
        h.assertTrue(allowed == TribeStanding.KILL_CAP_PER_DAY, "Kill gains cap at 20 per day, got " + allowed);
        h.assertTrue(data.tryKillGain(id, TribeDefinition.SOIL, day + 1, TribeStanding.KILL_CAP_PER_DAY), "A new day resets the kill cap");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hearthOfferingRaisesStanding(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2);
        h.setBlock(rel, TribeRegistry.TRIBE_HEARTH.get());
        BlockPos pos = h.absolutePos(rel);
        var hearth = (TribeHearthBlockEntity) h.getLevel().getBlockEntity(pos);
        hearth.setTribe(TribeDefinition.STONE);
        var player = h.makeMockServerPlayerInLevel();
        int before = TribeStanding.get(player.server, player.getUUID(), TribeDefinition.STONE);
        ItemStack ore = new ItemStack(Items.RAW_IRON, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, ore);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        h.getBlockState(rel).useItemOn(ore, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        int after = TribeStanding.get(player.server, player.getUUID(), TribeDefinition.STONE);
        h.assertTrue(after - before == TribeStanding.GAIN_FAVOURED, "Raw iron is favoured by the Grit-singers: +3, got " + (after - before));
        // The GameTest mock ServerPlayer hard-codes isCreative() == true, so the block path must NOT consume for it...
        h.assertTrue(ore.getCount() == 4, "Creative offerings are free");
        // ...while the survival path (what a real player right-clicking gets) consumes exactly one.
        int survivalBefore = TribeStanding.get(player.server, player.getUUID(), TribeDefinition.STONE);
        h.assertTrue(TribeHearthBlock.offer(player, hearth, ore, true) == TribeStanding.GAIN_FAVOURED, "Survival offering grants +3");
        h.assertTrue(ore.getCount() == 3, "One offering is consumed");
        after = TribeStanding.get(player.server, player.getUUID(), TribeDefinition.STONE);
        h.assertTrue(after - survivalBefore == TribeStanding.GAIN_FAVOURED, "Survival offering raises standing");
        ItemStack echo = new ItemStack(ModItems.ATTUNED_ECHO.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, echo);
        h.getBlockState(rel).useItemOn(echo, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        int reagent = TribeStanding.get(player.server, player.getUUID(), TribeDefinition.STONE);
        h.assertTrue(reagent - after == TribeStanding.GAIN_REAGENT, "Attuned Echo is the Grit-singers' reagent tier: +8");
        h.assertTrue(TribeDefinition.STONE.offeringValue(new ItemStack(Items.STONE)) == 0, "Plain stone is not an offering");
        h.assertTrue(TribeDefinition.SOIL.offeringValue(new ItemStack(Items.COOKED_BEEF)) == TribeStanding.GAIN_FOOD, "Any food gives +1");
        h.assertTrue(hearth.signal() == 0, "A Stranger reads 0 on the comparator");
        ItemStack drop = h.getBlockState(rel).getBlock().getCloneItemStack(h.getBlockState(rel), hit, h.getLevel(), pos, player);
        h.assertTrue(TribeDefinition.of(drop) == TribeDefinition.STONE, "The hearth item keeps its tribe");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void kinshipTotemCountsAsVoice(GameTestHelper h) {
        h.setBlock(2, 2, 2, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(4, 2, 2, TribeRegistry.KINSHIP_TOTEM.get().defaultBlockState().setValue(KinshipTotemBlock.TRIBE, TribeDefinition.SIGIL.ordinal()));
        h.setBlock(6, 2, 2, TribeRegistry.KINSHIP_TOTEM.get().defaultBlockState().setValue(KinshipTotemBlock.TRIBE, TribeDefinition.SIGIL.ordinal()));
        h.setBlock(4, 2, 4, TribeRegistry.KINSHIP_TOTEM.get().defaultBlockState().setValue(KinshipTotemBlock.TRIBE, TribeDefinition.SPARK.ordinal()));
        BlockPos origin = h.absolutePos(new BlockPos(3, 2, 3));
        h.assertTrue(LatticeNetwork.countKinshipTribes(h.getLevel(), origin, 8) == 2, "Two distinct tribes among three kinship totems");
        var voices = LatticeNetwork.collectAttunements(h.getLevel(), origin, 8);
        h.assertTrue(voices.contains(Attunement.SPIRIT) && voices.contains(Attunement.FIRE) && voices.contains(Attunement.EARTH),
                "Kinship totems lend their tribe's attunement, got " + voices);
        h.setBlock(3, 2, 3, ModBlocks.PULSE_RESONATOR.get());
        var resonator = (PulseResonatorBlockEntity) h.getLevel().getBlockEntity(origin);
        resonator.setItem(PulseResonatorBlockEntity.SLOT, new ItemStack(ModItems.ECHO_SHARD.get()));
        h.runAfterDelay(PulseResonatorBlockEntity.GAIN_INTERVAL + 2, () -> {
            h.assertTrue(resonator.getHarmonics() == 3, "Earth totem + two kinship tribes = 3 voices, got " + resonator.getHarmonics());
            h.assertTrue(resonator.getPulseStored() > 0, "The resonator sounds on tribe voices alone");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void elderOffersScaleWithRank(GameTestHelper h) {
        var kin = TribeRegistry.TRIBAL_KIN.get().create(h.getLevel());
        kin.setTribe(TribeDefinition.SPINDLE);
        kin.setRole(KinRole.ELDER);
        h.assertTrue(kin.buildOffers(TribeRank.STRANGER).isEmpty(), "Strangers get no offers");
        h.assertTrue(kin.buildOffers(TribeRank.GUEST).size() == 2, "Guests get two offers");
        h.assertTrue(kin.buildOffers(TribeRank.FRIEND).size() == 4, "Friends get four offers");
        h.assertTrue(kin.buildOffers(TribeRank.KIN).size() == 6 && kin.buildOffers(TribeRank.VOICE).size() == 6, "Kin and Voice get all six");
        boolean thread = kin.buildOffers(TribeRank.FRIEND).stream().anyMatch(o -> o.getResult().is(ModItems.LOOM_THREAD.get()));
        // 3.1: the Loom-stitchers' Kin counter now sells their own voice's generator (design 3.1 section 12).
        boolean anchor = kin.buildOffers(TribeRank.KIN).stream()
                .anyMatch(o -> o.getResult().is(tk.darrow.tribalpower.generator.GeneratorRegistry.LOOM_ANCHOR_ITEM.get()));
        h.assertTrue(thread && anchor, "Loom-stitchers sell Loom Thread at Friend and a Loom Anchor at Kin");
        for (TribeDefinition tribe : TribeDefinition.values()) {
            h.assertTrue(tribe.trades().size() == 6, tribe.id() + " must have six offers");
            h.assertTrue(tribe.tradesFor(TribeRank.GUEST).size() == 2 && tribe.tradesFor(TribeRank.FRIEND).size() == 4, tribe.id() + " offers two per rank");
        }
        var tag = new net.minecraft.nbt.CompoundTag();
        kin.addAdditionalSaveData(tag);
        h.assertTrue(tag.getInt("Tribe") == 8 && tag.getString("Role").equals("ELDER"), "NBT contract: Tribe int and Role string");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyVoiceKeepingTribeSellsItsOwnGenerator(GameTestHelper h) {
        // Standing finally pays: each tribe that keeps a voice sells that voice's craft (design 3.1 section 12).
        record Sold(TribeDefinition tribe, TribeRank rank, net.minecraft.world.item.Item item) {}
        var expected = new Sold[]{
                new Sold(TribeDefinition.SOIL, TribeRank.FRIEND, ModItems.STONE_FONT.get()),
                new Sold(TribeDefinition.STONE, TribeRank.GUEST, ModItems.RESONANCE_MESH.get()),
                new Sold(TribeDefinition.SPARK, TribeRank.FRIEND,
                        tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN_ITEM.get()),
                new Sold(TribeDefinition.SPROUT, TribeRank.FRIEND,
                        tk.darrow.tribalpower.generator.GeneratorRegistry.WAVE_DRUM_ITEM.get()),
                new Sold(TribeDefinition.CLOCK, TribeRank.KIN,
                        tk.darrow.tribalpower.generator.GeneratorRegistry.WIND_HARP_ITEM.get()),
                new Sold(TribeDefinition.SIGIL, TribeRank.KIN,
                        tk.darrow.tribalpower.generator.GeneratorRegistry.WAKE_BELL_ITEM.get()),
                new Sold(TribeDefinition.SPINDLE, TribeRank.KIN,
                        tk.darrow.tribalpower.generator.GeneratorRegistry.LOOM_ANCHOR_ITEM.get())};
        for (Sold sold : expected) {
            boolean found = sold.tribe().tradesFor(sold.rank()).stream()
                    .anyMatch(o -> o.result().get().is(sold.item()));
            h.assertTrue(found, sold.tribe().id() + " must sell " + sold.item() + " at " + sold.rank());
            boolean tooEarly = sold.rank() == TribeRank.GUEST || sold.tribe().tradesFor(
                    TribeRank.values()[sold.rank().ordinal() - 1]).stream().noneMatch(o -> o.result().get().is(sold.item()));
            h.assertTrue(tooEarly, sold.item() + " must not be available before " + sold.rank());
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hearthOfferingsAreCappedPerDayPerTribe(GameTestHelper h) {
        // Without this cap, renewable ore feeds the hearth that gates the pit that makes the ore.
        var data = new tk.darrow.tribalpower.tribe.TribeStandingSavedData();
        var player = java.util.UUID.randomUUID();
        int cap = tk.darrow.tribalpower.tribe.TribeStanding.OFFER_CAP_PER_DAY;

        int granted = 0;
        for (int i = 0; i < 40; i++)
            granted += data.allowOffering(player, TribeDefinition.STONE, 0L, cap, 8);
        h.assertTrue(granted == cap, "A day of offerings must stop at " + cap + ", granted " + granted);
        h.assertTrue(data.allowOffering(player, TribeDefinition.STONE, 0L, cap, 8) == 0,
                "Once the day is spent the hearth takes nothing more");

        // The cap is per tribe, and it resets with the day.
        h.assertTrue(data.allowOffering(player, TribeDefinition.SPARK, 0L, cap, 8) == 8,
                "One tribe being sated must not close another");
        h.assertTrue(data.allowOffering(player, TribeDefinition.STONE, 1L, cap, 8) == 8,
                "A new day must open the hearth again");
        h.assertTrue(data.offeringsToday(player, TribeDefinition.STONE, 1L) == 8,
                "The new day must count from zero");
        h.succeed();
    }
}
