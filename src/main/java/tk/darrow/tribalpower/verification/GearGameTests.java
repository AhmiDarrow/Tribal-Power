package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.echo.ProcessingRecipes;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritGear;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class GearGameTests {

    @GameTest(template = "empty")
    public static void diamondPickMinesObsidian(GameTestHelper h) {
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        h.assertTrue(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                "Spiritgear pick must be diamond-tier for obsidian");
        h.assertTrue(pick.isCorrectToolForDrops(Blocks.DEEPSLATE.defaultBlockState()),
                "Spiritgear pick must mine deepslate");
        h.assertTrue(pick.getMaxDamage() >= SpiritGear.TOOL_DURABILITY,
                "Spiritgear tools last at least 1024, got " + pick.getMaxDamage());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sneakUseWritesAndOverwritesVoice(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        ItemStack cell = PulseCellItem.createFilled(200);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        player.getInventory().setItem(1, cell);
        player.setShiftKeyDown(true);
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(pick, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(SpiritGear.voice(pick).orElse(null) == Attunement.EARTH, "Earth totem writes GearVoice=earth");
        h.assertTrue(PulseCellItem.getPulse(cell) == 160, "Link spends 40 Pulse");

        h.setBlock(pos, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(pick, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(SpiritGear.voice(pick).orElse(null) == Attunement.FIRE, "Fire totem overwrites the voice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void linkWithoutPulseFails(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        player.setShiftKeyDown(true);
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(pick, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(SpiritGear.voice(pick).isEmpty(), "Link without Pulse must fail");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void attunePreservesVoiceAndBindRefusesRankZero(GameTestHelper h) {
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        SpiritGear.setVoice(pick, Attunement.EARTH);
        h.assertTrue(ProcessingRecipes.find(h.getLevel(), "echo_bind", pick) == null, "Bind refuses rank 0");
        var attune = ProcessingRecipes.find(h.getLevel(), "echo_attune", pick);
        h.assertTrue(attune != null, "Attune accepts rank 0");
        h.assertTrue(SpiritGear.rank(attune.result()) == 1, "Attune writes rank 1");
        h.assertTrue(SpiritGear.voice(attune.result()).orElse(null) == Attunement.EARTH, "Attune preserves voice");
        SpiritGear.setRank(pick, 1);
        var bind = ProcessingRecipes.find(h.getLevel(), "echo_bind", pick);
        h.assertTrue(bind != null && SpiritGear.rank(bind.result()) == 2, "Bind raises rank 1 to 2");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void earthPickOpensThreeByThreeOnce(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        SpiritGear.setVoice(pick, Attunement.EARTH);
        ItemStack cell = PulseCellItem.createFilled(200);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        player.setItemInHand(InteractionHand.OFF_HAND, cell);
        BlockPos center = new BlockPos(4, 2, 4);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            h.setBlock(center.offset(x, 0, z), Blocks.STONE);
        }
        player.setPos(h.absolutePos(center.above(2)).getX() + 0.5,
                h.absolutePos(center.above(2)).getY(),
                h.absolutePos(center.above(2)).getZ() + 0.5);
        player.setYRot(0);
        player.setXRot(90);
        boolean broken = player.gameMode.destroyBlock(h.absolutePos(center));
        h.assertTrue(broken, "Center stone must break");
        int gone = 0;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (h.getBlockState(center.offset(x, 0, z)).isAir()) gone++;
        }
        h.assertTrue(gone >= 8, "Earth pick 3x3 should clear the stone plane, got " + gone);
        h.assertTrue(PulseCellItem.getPulse(cell) == 198, "3x3 spends Pulse once (rank 0 mine cost 2)");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void firePickSmeltsIronOre(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        SpiritGear.setVoice(pick, Attunement.FIRE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        player.setItemInHand(InteractionHand.OFF_HAND, PulseCellItem.createFilled(200));
        BlockPos ore = new BlockPos(2, 1, 2);
        h.setBlock(ore, Blocks.IRON_ORE);
        var smelts = h.getLevel().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.SMELTING,
                new net.minecraft.world.item.crafting.SingleRecipeInput(
                        new ItemStack(net.minecraft.world.item.Items.RAW_IRON)),
                h.getLevel());
        h.assertTrue(smelts.isPresent(), "GameTest world must know raw iron smelts");
        var abs = h.absolutePos(ore);
        var state = h.getBlockState(ore);
        SpiritGear.beginSwing(player, pick, true, false);
        try {
            net.minecraft.world.level.block.Block.dropResources(state, h.getLevel(), abs, null, player, pick);
            h.setBlock(ore, Blocks.AIR);
        } finally {
            SpiritGear.endSwing();
        }
        int ingots = 0, raws = 0;
        for (var item : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                h.getBounds().inflate(8))) {
            if (item.getItem().is(net.minecraft.world.item.Items.IRON_INGOT)) ingots += item.getItem().getCount();
            if (item.getItem().is(net.minecraft.world.item.Items.RAW_IRON)) raws += item.getItem().getCount();
        }
        h.assertTrue(ingots > 0 && raws == 0,
                "Fire pick must smelt iron ore into ingots (ingots=" + ingots + " raws=" + raws + ")");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unweaveDamagedRankedPickDropsPlainIngot(GameTestHelper h) {
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        SpiritGear.setRank(pick, 3);
        SpiritGear.setVoice(pick, Attunement.FIRE);
        pick.setDamageValue(12);
        var formula = ProcessingRecipes.find(h.getLevel(), "echo_unweave", pick);
        h.assertTrue(formula != null && formula.result().is(ModItems.MANIFESTED_INGOT.get()),
                "Damaged ranked pick must unweave to one ingot");
        h.assertTrue(SpiritGear.rank(formula.result()) == 0 && SpiritGear.voice(formula.result()).isEmpty(),
                "Unweave must strip rank and voice");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void fireHoodGrantsFireResistNotNightVision(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        ItemStack hood = new ItemStack(ModItems.SPIRITWEAVE_HOOD.get());
        SpiritGear.setVoice(hood, Attunement.FIRE);
        player.setItemSlot(EquipmentSlot.HEAD, hood);
        player.getInventory().setItem(1, PulseCellItem.createFilled(200));
        long mod = h.getLevel().getGameTime() % 80;
        int wait = mod == 0 ? 0 : (int) (80 - mod);
        Runnable check = () -> {
            ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
            worn.getItem().inventoryTick(worn, h.getLevel(), player, 39, false);
            h.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "Fire hood grants fire resistance");
            h.assertFalse(player.hasEffect(MobEffects.NIGHT_VISION), "Fire hood replaces night vision");
            h.succeed();
        };
        if (wait == 0) check.run();
        else h.runAfterDelay(wait, check);
    }

    @GameTest(template = "empty")
    public static void gearRanksAreCostlyAndBindRefusesZero(GameTestHelper h) {
        ItemStack station = new ItemStack(ModItems.ECHO_SHATTER.get());
        h.assertTrue(ProcessingRecipes.find(h.getLevel(), "echo_bind", station) == null, "Machine Bind refuses rank 0");
        var attune = ProcessingRecipes.find(h.getLevel(), "echo_attune", station);
        h.assertTrue(attune != null && attune.seconds() == 16 && attune.pulse() == 48 && attune.catalysts().isEmpty(),
                "Machine Attune is 16s/48 Pulse and needs no catalyst");
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        var gear = ProcessingRecipes.find(h.getLevel(), "echo_attune", pick);
        h.assertTrue(gear != null && gear.seconds() == 45 && gear.pulse() == 48, "Gear Attune is 45s at 48 Pulse a second");
        ItemStack spent = pick.copy();
        spent.shrink(1);
        h.assertTrue(ProcessingRecipes.find(h.getLevel(), "echo_attune", spent) == null, "A spent piece is not ranked again");
        h.assertTrue(gear.catalysts().size() == 1 && gear.catalysts().getFirst().is(ModItems.ATTUNED_ECHO.get())
                && gear.catalysts().getFirst().getCount() == 4, "Gear Attune consumes 4 Attuned Echo");
        ItemStack bound = SpiritGear.withRank(pick, 2);
        var manifest = ProcessingRecipes.find(h.getLevel(), "echo_manifest", bound);
        h.assertTrue(manifest != null && manifest.seconds() == 180 && manifest.pulse() == 96, "Gear Manifest is 180s at 96 Pulse a second");
        h.assertTrue(manifest.catalysts().stream().anyMatch(s -> s.is(ModItems.LOOM_THREAD.get()) && s.getCount() == 4)
                && manifest.catalysts().stream().anyMatch(s -> s.is(ModItems.RESONANT_CORE.get()) && s.getCount() == 2),
                "Manifest consumes 2 Resonant Cores and 4 Loom Thread (a boss drop)");
        h.succeed();
    }

    /** A station waits for its catalysts, and uses them up when the rank lands. */
    @GameTest(template = "empty", timeoutTicks = 1400)
    public static void aRankNeedsAndSpendsItsCatalysts(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.ECHO_ATTUNE.get());
        h.setBlock(3, 2, 2, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        h.setBlock(2, 2, 3, ModBlocks.PULSE_RESONATOR.get());
        var resonator = (tk.darrow.tribalpower.api.pulse.PulseHandler) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 3)));
        var station = (tk.darrow.tribalpower.blockentity.EchoStationBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        station.setItem(0, new ItemStack(ModItems.SPIRITGEAR_BLADE.get()));
        h.runAfterDelay(45, () -> {
            h.assertTrue(station.getItem(0).is(ModItems.SPIRITGEAR_BLADE.get()) && station.work() == 0,
                    "Without catalysts the station must wait, not work");
            station.setItem(tk.darrow.tribalpower.blockentity.EchoStationBlockEntity.CATALYST_A, new ItemStack(ModItems.ATTUNED_ECHO.get(), 5));
        });
        h.onEachTick(() -> resonator.insertPulse(200, false));
        h.succeedWhen(() -> {
            ItemStack done = java.util.stream.IntStream.rangeClosed(1, 8).mapToObj(station::getItem)
                    .filter(s -> s.is(ModItems.SPIRITGEAR_BLADE.get())).findFirst().orElse(ItemStack.EMPTY);
            h.assertTrue(!done.isEmpty() && SpiritGear.rank(done) == 1, "The blade must come out Attuned");
            h.assertTrue(station.getItem(tk.darrow.tribalpower.blockentity.EchoStationBlockEntity.CATALYST_A).getCount() == 1,
                    "Exactly four Attuned Echo must be used");
        });
    }

    /** A piece spends its own seated cell first, reaches for carried cells only once it runs dry, and takes a bigger cell. */
    @GameTest(template = "empty")
    public static void gearSpendsItsOwnCellFirst(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        ItemStack seated = new ItemStack(ModItems.PULSE_CELL.get());
        tk.darrow.tribalpower.item.PulseCellItem.setPulse(seated, 50);
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.offer(pick, seated) == ItemStack.EMPTY, "An empty piece takes the cell whole");
        ItemStack carried = new ItemStack(ModItems.PULSE_CELL.get());
        tk.darrow.tribalpower.item.PulseCellItem.setPulse(carried, 100);
        player.getInventory().add(carried);
        ItemStack held = player.getInventory().items.stream().filter(s -> s.is(ModItems.PULSE_CELL.get())).findFirst().orElseThrow();

        h.assertTrue(tk.darrow.tribalpower.item.GearCell.spend(player, pick, 30), "30 Pulse is affordable");
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.pulse(pick) == 20 && tk.darrow.tribalpower.item.PulseCellItem.getPulse(held) == 100,
                "The carried cell must stay untouched while the seated cell has charge");
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.spend(player, pick, 30), "The rest comes from the carried cell");
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.pulse(pick) == 0 && tk.darrow.tribalpower.item.PulseCellItem.getPulse(held) == 90,
                "Only the shortfall comes from the carried cell, got " + tk.darrow.tribalpower.item.PulseCellItem.getPulse(held));

        ItemStack greater = new ItemStack(ModItems.GREATER_PULSE_CELL.get());
        tk.darrow.tribalpower.item.PulseCellItem.setPulse(greater, 500);
        ItemStack back = tk.darrow.tribalpower.item.GearCell.offer(pick, greater);
        h.assertTrue(back != null && back.is(ModItems.PULSE_CELL.get()), "Upgrading hands the plain cell back");
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.capacity(pick) == 1200 && tk.darrow.tribalpower.item.GearCell.pulse(pick) == 500,
                "The Greater cell is seated with its charge");
        ItemStack topUp = new ItemStack(ModItems.PULSE_CELL.get());
        tk.darrow.tribalpower.item.PulseCellItem.setPulse(topUp, 100);
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.offer(pick, topUp) == topUp && tk.darrow.tribalpower.item.GearCell.pulse(pick) == 600
                && tk.darrow.tribalpower.item.PulseCellItem.getPulse(topUp) == 0, "A smaller cell tops the seated one up");
        h.succeed();
    }

    /** Ranks are worth having: a Manifested blade and a Manifested robe carry much more than plain ones. */
    @GameTest(template = "empty")
    public static void ranksRaiseTheNumbers(GameTestHelper h) {
        ItemStack plain = new ItemStack(ModItems.SPIRITGEAR_BLADE.get());
        double plainDamage = damage(plain), topDamage = damage(SpiritGear.withRank(plain, 3));
        h.assertTrue(topDamage - plainDamage >= 10 - 0.01, "A Manifested blade hits 10 harder, got " + plainDamage + " -> " + topDamage);
        ItemStack robe = SpiritGear.withRank(new ItemStack(ModItems.SPIRITWEAVE_ROBE.get()), 3);
        double health = sum(robe, EquipmentSlot.CHEST, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        double armor = sum(robe, EquipmentSlot.CHEST, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        h.assertTrue(health >= 4 - 0.01, "A Manifested robe adds 4 health, got " + health);
        h.assertTrue(armor >= 11 - 0.01, "A Manifested robe is 11 armour, got " + armor);
        h.succeed();
    }

    private static double damage(ItemStack stack) {
        return sum(stack, EquipmentSlot.MAINHAND, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    }

    /** Every modifier a stack gives in a slot, rank bonuses included. */
    private static double sum(ItemStack stack, EquipmentSlot slot,
                              net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> wanted) {
        double[] total = {0};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.is(wanted)) total[0] += modifier.amount();
        });
        return total[0];
    }

    @GameTest(template = "empty")
    public static void eightCharmsAndSkyGrantsFlight(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        h.assertTrue(tk.darrow.tribalpower.charm.CharmKind.values().length >= 10, "At least ten Spirit Charms");
        ItemStack sky = new ItemStack(ModItems.SKY_CHARM.get());
        h.assertTrue(tk.darrow.tribalpower.charm.SpiritCharmItem.grantsFlight(sky), "Sky Charm Air is creative flight");
        player.getInventory().setItem(1, PulseCellItem.createFilled(200));
        h.assertTrue(tk.darrow.tribalpower.charm.CharmSlots.of(player).equip(sky.copy()), "Charm equips into our slot");
        h.assertTrue(!tk.darrow.tribalpower.charm.CharmSlots.equipped(player).isEmpty(), "Worn charm is visible");
        ItemStack chorus = new ItemStack(ModItems.CHORUS_CHARM.get());
        h.assertTrue(tk.darrow.tribalpower.charm.SpiritCharmItem.voices(chorus).isEmpty(), "Chorus starts silent");
        h.assertTrue(!tk.darrow.tribalpower.charm.SpiritCharmItem.grantsFlight(new ItemStack(ModItems.HEARTH_CHARM.get())),
                "Hearth is food, not flight");
        h.assertTrue(!tk.darrow.tribalpower.charm.SpiritCharmItem.grantsFlight(new ItemStack(ModItems.VEIL_CHARM.get())),
                "Veil is stealth, not flight");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void refundPaysTheOffhandCellBack(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack robe = new ItemStack(ModItems.SPIRITWEAVE_ROBE.get());
        ItemStack cell = new ItemStack(ModItems.PULSE_CELL.get());
        PulseCellItem.setPulse(cell, 40);
        player.setItemSlot(EquipmentSlot.OFFHAND, cell);
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.spend(player, robe, 8), "The offhand cell can pay");
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 32,
                "Spend takes the offhand cell, left " + PulseCellItem.getPulse(player.getOffhandItem()));
        int returned = tk.darrow.tribalpower.item.GearCell.refund(player, robe, 8);
        h.assertTrue(returned == 8, "The refund finds a home, returned " + returned);
        h.assertTrue(PulseCellItem.getPulse(player.getOffhandItem()) == 40,
                "The offhand cell is paid back, holds " + PulseCellItem.getPulse(player.getOffhandItem()));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void switchedOffSpiritweaveGivesNothingAndSpendsNothing(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack boots = new ItemStack(ModItems.SPIRITWEAVE_BOOTS.get());
        ItemStack cell = PulseCellItem.createFilled(200);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        player.getInventory().setItem(1, cell);
        player.fallDistance = 3;
        boots.getItem().inventoryTick(boots, h.getLevel(), player, 36, false);
        h.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "Worn boots soften a fall");
        int after = PulseCellItem.getPulse(cell);
        h.assertTrue(after < 200, "The fall cost Pulse");
        player.removeEffect(MobEffects.SLOW_FALLING);

        tk.darrow.tribalpower.item.GearSettingsPayload.apply(player, tk.darrow.tribalpower.item.GearSettingsPayload.ARMOR, EquipmentSlot.FEET.ordinal(), false);
        h.assertTrue(SpiritGear.abilitiesOff(boots), "The Gear screen switched the boots off");
        player.fallDistance = 3;
        boots.getItem().inventoryTick(boots, h.getLevel(), player, 36, false);
        h.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING), "Switched-off boots give no effect");
        h.assertTrue(PulseCellItem.getPulse(cell) == after, "Switched-off boots draw no Pulse");
        h.assertTrue(player.getItemBySlot(EquipmentSlot.FEET) == boots, "The piece stays worn");

        tk.darrow.tribalpower.item.GearSettingsPayload.apply(player, tk.darrow.tribalpower.item.GearSettingsPayload.ARMOR, EquipmentSlot.FEET.ordinal(), true);
        h.assertFalse(SpiritGear.abilitiesOff(boots), "And on again");
        tk.darrow.tribalpower.item.GearSettingsPayload.apply(player, tk.darrow.tribalpower.item.GearSettingsPayload.ARMOR, EquipmentSlot.HEAD.ordinal(), false);
        h.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "An empty slot is left alone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staffVoiceHotkeyCyclesLikeSneakUse(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        ItemStack staff = new ItemStack(ModItems.SPIRIT_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        Attunement before = tk.darrow.tribalpower.item.SpiritStaffItem.element(staff);
        tk.darrow.tribalpower.item.GearSettingsPayload.apply(player, tk.darrow.tribalpower.item.GearSettingsPayload.STAFF_VOICE, 0, false);
        Attunement after = tk.darrow.tribalpower.item.SpiritStaffItem.element(staff);
        h.assertTrue(after.ordinal() == (before.ordinal() + 1) % Attunement.values().length, "The hotkey steps the voice once");
        tk.darrow.tribalpower.item.GearSettingsPayload.apply(player, tk.darrow.tribalpower.item.GearSettingsPayload.VAULT, 0, false);
        h.assertFalse(tk.darrow.tribalpower.camp.identity.Camps.personalVault(player), "The vault switch does nothing outside a camp");
        h.succeed();
    }
}
