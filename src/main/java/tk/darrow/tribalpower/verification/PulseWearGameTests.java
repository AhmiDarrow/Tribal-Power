package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.ModEffects;
import tk.darrow.tribalpower.item.GearCell;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.song.PulseBowItem;

/**
 * Spiritgear keeps its edge while Pulse pays: a paid block, use, blow or shot spends the cell and takes no wear, and
 * only an unpaid one wears the tool. Also the Loom blessing's tenth, which adds up over small spends, and Spiritweave's
 * reactive perks, which stop with the piece switched off or its cell dry.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class PulseWearGameTests {
    private PulseWearGameTests() {}

    /** The game's own survival fake player, emptied, holding {@code tool}, with {@code pulse} in a carried cell (none at 0). */
    private static FakePlayer worker(GameTestHelper h, ItemStack tool, int pulse) {
        FakePlayer player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        BlockPos stand = h.absolutePos(new BlockPos(1, 2, 1));
        player.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        if (pulse > 0) player.getInventory().setItem(9, PulseCellItem.createFilled(pulse));
        return player;
    }

    private static int carried(FakePlayer player) {
        return PulseCellItem.getPulse(player.getInventory().getItem(9));
    }

    @GameTest(template = "empty")
    public static void aPaidBlockSpendsPulseAndLeavesTheEdge(GameTestHelper h) {
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        FakePlayer player = worker(h, pick, 200);
        BlockPos stone = new BlockPos(3, 1, 3);
        h.setBlock(stone, Blocks.STONE);
        h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(stone)), "The stone breaks");
        h.assertTrue(pick.getDamageValue() == 0, "A paid block takes no wear, damage " + pick.getDamageValue());
        h.assertTrue(carried(player) == 198, "It spends the mining cost of 2 Pulse, cell at " + carried(player));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anUnpaidBlockWearsTheEdge(GameTestHelper h) {
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        FakePlayer player = worker(h, pick, 0);
        BlockPos stone = new BlockPos(3, 1, 3);
        h.setBlock(stone, Blocks.STONE);
        h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(stone)), "A starved pick still mines");
        // a point as any pick takes, and one more for starving below Bound
        h.assertTrue(pick.getDamageValue() == 2, "An unpaid block wears the pick, damage " + pick.getDamageValue());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aPaidBlowSpendsPulseAndLeavesTheBlade(GameTestHelper h) {
        ItemStack blade = new ItemStack(ModItems.SPIRITGEAR_BLADE.get());
        FakePlayer player = worker(h, blade, 200);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 1));
        player.attack(pig);
        h.assertTrue(pig.getHealth() < pig.getMaxHealth(), "The blow lands");
        h.assertTrue(blade.getDamageValue() == 0, "A paid blow takes no wear, damage " + blade.getDamageValue());
        h.assertTrue(carried(player) == 200 - SpiritGear.hitCost(blade), "It spends the hit cost, cell at " + carried(player));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anUnpaidBlowWearsTheBlade(GameTestHelper h) {
        ItemStack blade = new ItemStack(ModItems.SPIRITGEAR_BLADE.get());
        FakePlayer player = worker(h, blade, 0);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 1));
        player.attack(pig);
        h.assertTrue(pig.getHealth() < pig.getMaxHealth(), "The starved blow still lands");
        h.assertTrue(blade.getDamageValue() == 2, "An unpaid blow wears the blade like a sword, and a point for starving, damage " + blade.getDamageValue());
        h.succeed();
    }

    /** Stripping a log through the game's own click: paid, the axe keeps its edge; starved, it wears. */
    @GameTest(template = "empty")
    public static void aStripIsPaidOrWears(GameTestHelper h) {
        BlockPos log = new BlockPos(3, 1, 3);
        for (int pulse : new int[]{200, 0}) {
            h.setBlock(log, Blocks.OAK_LOG);
            ItemStack axe = new ItemStack(ModItems.SPIRITGEAR_AXE.get());
            FakePlayer player = worker(h, axe, pulse);
            BlockPos at = h.absolutePos(log);
            var hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
            player.gameMode.useItemOn(player, h.getLevel(), axe, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(h.getBlockState(log).is(Blocks.STRIPPED_OAK_LOG), "The log is stripped (" + pulse + " Pulse)");
            if (pulse > 0) {
                h.assertTrue(axe.getDamageValue() == 0, "A paid strip takes no wear, damage " + axe.getDamageValue());
                h.assertTrue(carried(player) == pulse - SpiritGear.useCost(axe), "It spends the use cost, cell at " + carried(player));
            } else {
                h.assertTrue(axe.getDamageValue() == 2, "An unpaid strip wears the axe, damage " + axe.getDamageValue());
            }
        }
        h.succeed();
    }

    /** No Pulse, no song: a starved draw shoots nothing and so wears nothing; a paid one spends and wears nothing either. */
    @GameTest(template = "empty")
    public static void aShotIsPaidInPulseNotWear(GameTestHelper h) {
        ItemStack bow = new ItemStack(ModItems.PULSE_BOW.get());
        FakePlayer player = worker(h, bow, 0);
        var item = (PulseBowItem) bow.getItem();
        item.releaseUsing(bow, h.getLevel(), player, item.getUseDuration(bow, player) - 20);
        h.assertTrue(bow.getDamageValue() == 0, "A starved draw shoots nothing and wears nothing");
        player.getInventory().setItem(9, PulseCellItem.createFilled(100));
        item.releaseUsing(bow, h.getLevel(), player, item.getUseDuration(bow, player) - 20);
        h.assertTrue(carried(player) == 100 - TribalConfig.bowPulse(), "A full draw spends bowPulse, cell at " + carried(player));
        h.assertTrue(bow.getDamageValue() == 0, "A paid shot takes no wear, damage " + bow.getDamageValue());
        h.getLevel().getEntities(tk.darrow.tribalpower.entity.ModEntities.SONIC_BOLT.get(), h.getBounds().inflate(16), b -> true)
                .forEach(net.minecraft.world.entity.Entity::discard);
        h.succeed();
    }

    /** The Loom blessing gives back a tenth (a fifth at II) of what gear spends, exactly, even a Pulse at a time. */
    @GameTest(template = "empty")
    public static void loomBlessingReturnsItsShareOverSmallSpends(GameTestHelper h) {
        double share = TribalConfig.loomBlessingRefund();
        for (int level : new int[]{1, 2}) {
            for (int each : new int[]{1, 3}) {
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                ModEffects.bless(player, Attunement.LOOM, 20 * 60, level - 1, false);
                h.assertTrue(ModEffects.blessingLevel(player, Attunement.LOOM) == level, "Blessed at level " + level);
                ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
                ItemStack cell = new ItemStack(ModItems.PULSE_CELL.get());
                PulseCellItem.setPulse(cell, 1000);
                GearCell.offer(pick, cell);
                int spends = 100, spent = spends * each;
                for (int i = 0; i < spends; i++) h.assertTrue(GearCell.spend(player, pick, each), "Spend " + i + " is paid");
                int back = (int) Math.round(spent * share * level);
                h.assertTrue(GearCell.pulse(pick) == 1000 - spent + back,
                        "Level " + level + ", " + spends + " spends of " + each + ": " + back + " should come back, the cell holds "
                                + GearCell.pulse(pick) + " of " + (1000 - spent));
            }
        }
        h.succeed();
    }

    /** An Air robe turns knockback aside while it is on and paid, and not when switched off or with its cell dry. */
    @GameTest(template = "empty")
    public static void switchedOffSpiritweaveStopsAReactivePerk(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cell = PulseCellItem.createFilled(200);
        player.getInventory().setItem(9, cell);
        ItemStack robe = new ItemStack(ModItems.SPIRITWEAVE_ROBE.get());
        SpiritGear.setVoice(robe, Attunement.AIR);
        player.setItemSlot(EquipmentSlot.CHEST, robe);

        player.setDeltaMovement(Vec3.ZERO);
        player.knockback(0.5, 1, 0);
        h.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6, "The Air robe turns the knockback aside, moved " + player.getDeltaMovement());
        h.assertTrue(PulseCellItem.getPulse(cell) == 200 - SpiritGear.armorCost(robe), "Its upkeep is paid, cell at " + PulseCellItem.getPulse(cell));
        player.knockback(0.5, 1, 0);
        h.assertTrue(PulseCellItem.getPulse(cell) == 200 - SpiritGear.armorCost(robe), "One payment covers the next four seconds");

        SpiritGear.setAbilitiesOff(robe, true);
        player.knockback(0.5, 1, 0);
        h.assertTrue(player.getDeltaMovement().lengthSqr() > 1.0E-4, "Switched off, the robe lets the knockback through");
        h.assertTrue(PulseCellItem.getPulse(cell) == 200 - SpiritGear.armorCost(robe), "Switched off, it draws no Pulse");

        // A fresh robe of the same voice with nothing to pay its upkeep from does nothing either.
        PulseCellItem.setPulse(cell, 0);
        ItemStack dry = new ItemStack(ModItems.SPIRITWEAVE_ROBE.get());
        SpiritGear.setVoice(dry, Attunement.AIR);
        player.setItemSlot(EquipmentSlot.CHEST, dry);
        player.setDeltaMovement(Vec3.ZERO);
        player.knockback(0.5, 1, 0);
        h.assertTrue(player.getDeltaMovement().lengthSqr() > 1.0E-4, "With its cell dry, the robe lets the knockback through");
        h.succeed();
    }

    /** Spirit boots cancel a hurtful fall while on, and not once switched off. */
    @GameTest(template = "empty")
    public static void switchedOffSpiritBootsLetAFallHurt(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(9, PulseCellItem.createFilled(200));
        ItemStack boots = new ItemStack(ModItems.SPIRITWEAVE_BOOTS.get());
        SpiritGear.setVoice(boots, Attunement.SPIRIT);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        var fall = new net.neoforged.neoforge.event.entity.living.LivingFallEvent(player, 8, 1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(fall);
        h.assertTrue(fall.isCanceled(), "Spirit boots cancel an eight-block fall");
        SpiritGear.setAbilitiesOff(boots, true);
        fall = new net.neoforged.neoforge.event.entity.living.LivingFallEvent(player, 8, 1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(fall);
        h.assertFalse(fall.isCanceled(), "Switched off, the boots let the fall through");
        h.succeed();
    }

    /** An unpaid Lantern charm stretches nothing; paid, it keeps a blessing half again as long. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anUnpaidLanternStretchesNothing(GameTestHelper h) {
        ServerPlayer player = VerificationPlayers.inLevel(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        ItemStack cell = new ItemStack(ModItems.PULSE_CELL.get());
        player.getInventory().setItem(1, cell);
        h.assertTrue(tk.darrow.tribalpower.charm.CharmSlots.of(player).equip(new ItemStack(ModItems.LANTERN_CHARM.get())), "The Lantern equips");
        // Mock players are never ticked, so drive the charm hook ourselves.
        h.onEachTick(() -> tk.darrow.tribalpower.charm.CharmHooks.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)));
        long now = h.getLevel().getGameTime();
        int pastDue = (int) (40 - now % 40) + 2;
        int stretched = (int) Math.round(100 * TribalConfig.lanternCharmStretch());
        h.runAfterDelay(pastDue, () -> {
            h.assertFalse(tk.darrow.tribalpower.charm.CharmHooks.upkeepPaid(player), "An empty cell leaves the charm unpaid");
            ModEffects.bless(player, Attunement.LOOM, 100, 0, false);
            var loom = player.getEffect(ModEffects.blessing(Attunement.LOOM));
            h.assertTrue(loom != null && loom.getDuration() == 100, "Unpaid, the Lantern does not stretch a blessing: " + loom);
            PulseCellItem.setPulse(cell, 200);
        });
        h.runAfterDelay(pastDue + 42, () -> {
            try {
                h.assertTrue(tk.darrow.tribalpower.charm.CharmHooks.upkeepPaid(player), "A charged cell pays the charm again");
                ModEffects.bless(player, Attunement.EARTH, 100, 0, false);
                var earth = player.getEffect(ModEffects.blessing(Attunement.EARTH));
                h.assertTrue(earth != null && earth.getDuration() == stretched, "Paid, the Lantern stretches a blessing to " + stretched + ": " + earth);
                h.succeed();
            } finally {
                h.getLevel().getServer().getPlayerList().remove(player);
            }
        });
    }
}
