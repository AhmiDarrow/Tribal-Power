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
        var player = h.makeMockServerPlayerInLevel();
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
        var player = h.makeMockServerPlayerInLevel();
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
        var player = h.makeMockServerPlayerInLevel();
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
        var player = h.makeMockServerPlayerInLevel();
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
        var player = h.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        ItemStack hood = new ItemStack(ModItems.SPIRITWEAVE_HOOD.get());
        SpiritGear.setVoice(hood, Attunement.FIRE);
        player.setItemSlot(EquipmentSlot.HEAD, hood);
        player.getInventory().setItem(1, PulseCellItem.createFilled(200));
        int wait = (int) ((80 - (h.getLevel().getGameTime() % 80)) % 80);
        h.runAfterDelay(Math.max(1, wait), () -> {
            ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
            worn.getItem().inventoryTick(worn, h.getLevel(), player, 39, false);
            h.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "Fire hood grants fire resistance");
            h.assertFalse(player.hasEffect(MobEffects.NIGHT_VISION), "Fire hood replaces night vision");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void machineRankCostsMoreThanGearAndBindRefusesZero(GameTestHelper h) {
        ItemStack station = new ItemStack(ModItems.ECHO_SHATTER.get());
        h.assertTrue(ProcessingRecipes.find(h.getLevel(), "echo_bind", station) == null, "Machine Bind refuses rank 0");
        var attune = ProcessingRecipes.find(h.getLevel(), "echo_attune", station);
        h.assertTrue(attune != null && attune.seconds() == 16 && attune.pulse() == 48,
                "Machine Attune is 16s/48 Pulse");
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        var gear = ProcessingRecipes.find(h.getLevel(), "echo_attune", pick);
        h.assertTrue(gear != null && gear.seconds() == 8 && gear.pulse() == 24, "Gear Attune stays 8s/24");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void eightCharmsAndSkyGrantsFlight(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
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
}
