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

    /**
     * The real click path: a sneaking player holding gear never reaches the block's own use (vanilla hands the
     * click to the item), so linking has to happen in the RightClickBlock hook. A bound axe takes the voice.
     */
    @GameTest(template = "empty")
    public static void sneakClickThroughTheGameLinksBoundGear(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        ItemStack axe = SpiritGear.withRank(new ItemStack(ModItems.SPIRITGEAR_AXE.get()), 2);
        ItemStack cell = PulseCellItem.createFilled(200);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        player.getInventory().setItem(1, cell);
        player.setShiftKeyDown(true);
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.RESONANCE_TOTEM_WATER.get());
        var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
        var result = player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        ItemStack held = player.getMainHandItem();
        h.assertTrue(SpiritGear.voice(held).orElse(null) == Attunement.WATER,
                "A sneak-click through the game links a bound axe to the Water totem (" + result + ")");
        h.assertTrue(SpiritGear.rank(held) == 2, "Linking keeps the axe's rank");
        h.assertTrue(PulseCellItem.getPulse(cell) == 160, "The link spends 40 Pulse once");
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
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.capacity(pick) == tk.darrow.tribalpower.item.PulseCellItem.GREATER_CAPACITY && tk.darrow.tribalpower.item.GearCell.pulse(pick) == 500,
                "The Greater cell is seated with its charge");
        ItemStack topUp = new ItemStack(ModItems.PULSE_CELL.get());
        tk.darrow.tribalpower.item.PulseCellItem.setPulse(topUp, 100);
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.offer(pick, topUp) == topUp && tk.darrow.tribalpower.item.GearCell.pulse(pick) == 600
                && tk.darrow.tribalpower.item.PulseCellItem.getPulse(topUp) == 0, "A smaller cell tops the seated one up");
        h.succeed();
    }

    /**
     * Fittings come back off: an empty-cursor right-click on a piece in the inventory takes its seated cell (charge
     * and all), then a hood's goggles as a Ley Lens, one per click. Driven through the real menu click.
     */
    @GameTest(template = "empty")
    public static void fittingsComeBackOffWithAnEmptyCursor(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack hood = new ItemStack(ModItems.SPIRITWEAVE_HOOD.get());
        SpiritGear.setGoggles(hood, true);
        ItemStack cell = new ItemStack(ModItems.GREATER_PULSE_CELL.get());
        tk.darrow.tribalpower.item.PulseCellItem.setPulse(cell, 321);
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.offer(hood, cell) == ItemStack.EMPTY, "The hood takes the cell");
        player.getInventory().setItem(0, hood);
        var menu = player.inventoryMenu;
        int slot = 36; // hotbar 0 in the player's own inventory menu
        menu.clicked(slot, 1, net.minecraft.world.inventory.ClickType.PICKUP, player);
        ItemStack first = menu.getCarried();
        h.assertTrue(first.is(ModItems.GREATER_PULSE_CELL.get()) && tk.darrow.tribalpower.item.PulseCellItem.getPulse(first) == 321,
                "The first click takes the cell out with its charge, got " + first);
        h.assertTrue(menu.getSlot(slot).getItem().is(ModItems.SPIRITWEAVE_HOOD.get()), "and leaves the hood where it was");
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.cell(menu.getSlot(slot).getItem()) == null, "with no cell in it");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(slot, 1, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().is(tk.darrow.tribalpower.ley.LeyRegistry.LEY_LENS.get()), "The next click gives the goggles back as a Ley Lens");
        h.assertFalse(SpiritGear.goggles(menu.getSlot(slot).getItem()), "and the hood has no goggles now");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(slot, 1, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().is(ModItems.SPIRITWEAVE_HOOD.get()), "With nothing fitted, a right-click picks the hood up as usual");
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

    /** A survival-paying wearer of a Gathering Charm on a stone floor, with a full cell to pay its upkeep. */
    private static net.minecraft.server.level.ServerPlayer gatherer(GameTestHelper h) {
        return gatherer(h, PulseCellItem.createFilled(200));
    }

    /** The same wearer, paying from the given cell. */
    private static net.minecraft.server.level.ServerPlayer gatherer(GameTestHelper h, ItemStack cell) {
        for (int x = 3; x <= 13; x++)
            for (int z = 3; z <= 13; z++) h.setBlock(x, 1, z, Blocks.STONE);
        var player = VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild = false;
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var at = h.absolutePos(new BlockPos(8, 2, 8));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        player.getInventory().setItem(1, cell);
        h.assertTrue(tk.darrow.tribalpower.charm.CharmSlots.of(player).equip(new ItemStack(ModItems.GATHERING_CHARM.get())),
                "The Gathering Charm equips");
        // Mock players are never ticked, so drive the charm hook ourselves.
        h.onEachTick(() -> tk.darrow.tribalpower.charm.CharmHooks.playerTick(
                new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)));
        return player;
    }

    /** A still stack resting on the floor at a spot relative to the test, free to be picked up. */
    private static net.minecraft.world.entity.item.ItemEntity restingDrop(GameTestHelper h, int x, int z) {
        var at = h.absolutePos(new BlockPos(x, 2, z));
        var drop = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
                new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 8), 0, 0, 0);
        h.getLevel().addFreshEntity(drop);
        return drop;
    }

    /** Vanilla's pickup box: the player's bounds grown by one block sideways and half a block up and down. */
    private static boolean inReach(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Entity drop) {
        return player.getBoundingBox().inflate(1.0, 0.5, 1.0).intersects(drop.getBoundingBox());
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void gatheringCharmDrawsADropIntoReach(GameTestHelper h) {
        var player = gatherer(h);
        var drop = restingDrop(h, 12, 8);
        h.assertTrue(!inReach(player, drop), "The stack starts out of reach");
        h.succeedWhen(() -> h.assertTrue(inReach(player, drop),
                "The worn charm draws a stack 4 blocks off into pickup reach, it sits at " + drop.position()));
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void gatheringCharmLeavesSneakingAndPinnedDropsAlone(GameTestHelper h) {
        var player = gatherer(h);
        player.setShiftKeyDown(true);
        var loose = restingDrop(h, 12, 8);
        var pinned = restingDrop(h, 8, 12);
        // The flag's presence is the request, so even a false value keeps it put.
        pinned.getPersistentData().putBoolean("PreventRemoteMovement", false);
        var reserved = restingDrop(h, 4, 8);
        reserved.setTarget(java.util.UUID.randomUUID());
        Vec3 looseStart = loose.position();
        Vec3 pinnedStart = pinned.position();
        Vec3 reservedStart = reserved.position();
        h.runAfterDelay(30, () -> {
            h.assertTrue(loose.position().distanceTo(looseStart) < 0.3,
                    "Sneaking pauses the charm, yet the stack moved to " + loose.position());
            player.setShiftKeyDown(false);
        });
        // Once the player stands, the loose stack comes in (the charm works) while the pinned one stays put.
        h.succeedWhen(() -> {
            h.assertTrue(!player.isShiftKeyDown() && inReach(player, loose), "The loose stack comes in once the player stands");
            h.assertTrue(pinned.position().distanceTo(pinnedStart) < 0.3,
                    "A PreventRemoteMovement stack is never pulled, yet it moved to " + pinned.position());
            h.assertTrue(reserved.position().distanceTo(reservedStart) < 0.3,
                    "A stack kept for another player is never pulled, yet it moved to " + reserved.position());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void gatheringCharmWaitsOutAPickupDelay(GameTestHelper h) {
        var player = gatherer(h);
        var thrown = restingDrop(h, 12, 8);
        thrown.setPickUpDelay(40);
        Vec3 start = thrown.position();
        h.runAfterDelay(30, () -> h.assertTrue(thrown.position().distanceTo(start) < 0.3,
                "A stack under a pickup delay stays where it lands, yet it moved to " + thrown.position()));
        // Once the delay runs out the charm takes it like any other.
        h.succeedWhen(() -> h.assertTrue(!thrown.hasPickUpDelay() && inReach(player, thrown),
                "The stack comes in once its pickup delay is over, it sits at " + thrown.position()));
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void gatheringCharmStopsWhenUnpaid(GameTestHelper h) {
        gatherer(h, new ItemStack(ModItems.PULSE_CELL.get()));
        // Upkeep falls due every 40 ticks and the charm runs on credit until then, so let a payment tick pass first.
        long now = h.getLevel().getGameTime();
        int pastDue = (int) (40 - now % 40) + 2;
        var drop = new net.minecraft.world.entity.item.ItemEntity[1];
        Vec3[] start = new Vec3[1];
        h.runAfterDelay(pastDue, () -> {
            drop[0] = restingDrop(h, 12, 8);
            start[0] = drop[0].position();
        });
        h.runAfterDelay(pastDue + 30, () -> {
            h.assertTrue(drop[0].position().distanceTo(start[0]) < 0.3,
                    "An unpaid charm pulls nothing, yet the stack moved to " + drop[0].position());
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void gatheringCharmIsNoWings(GameTestHelper h) {
        var player = gatherer(h);
        h.assertTrue(!tk.darrow.tribalpower.charm.SpiritCharmItem.grantsFlight(new ItemStack(ModItems.GATHERING_CHARM.get())),
                "The Gathering Charm's Air is its pull, not flight");
        // Past two payment ticks and one effect tick: the charm is paid for and has applied its voices.
        h.runAfterDelay(85, () -> {
            h.assertTrue(PulseCellItem.getPulse(player.getInventory().getItem(1)) < 200, "The worn charm was paid for");
            h.assertTrue(!player.getAbilities().mayfly, "A Gathering Charm alone grants no flight");
            h.assertTrue(!player.hasEffect(MobEffects.SLOW_FALLING), "A Gathering Charm alone grants no slow fall");
            h.succeed();
        });
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

    /** 2026-09-29 sweep: the Loom boots read zza, which the server never gets for a player on foot. */
    @GameTest(template = "empty")
    public static void loomBootsReadWalkingForwardFromReportedMovement(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.setYRot(0);   // facing south, +z
            player.setKnownMovement(new net.minecraft.world.phys.Vec3(0, 0, 0.065));
            h.assertTrue(tk.darrow.tribalpower.item.SpiritGearHooks.walkingForward(player), "Sneaking ahead counts as walking forward");
            player.setKnownMovement(new net.minecraft.world.phys.Vec3(0, 0, -0.065));
            h.assertFalse(tk.darrow.tribalpower.item.SpiritGearHooks.walkingForward(player), "Backing away does not");
            player.setKnownMovement(new net.minecraft.world.phys.Vec3(0.065, 0, 0));
            h.assertFalse(tk.darrow.tribalpower.item.SpiritGearHooks.walkingForward(player), "Nor does a sidestep");
            player.setKnownMovement(net.minecraft.world.phys.Vec3.ZERO);
            h.assertFalse(tk.darrow.tribalpower.item.SpiritGearHooks.walkingForward(player), "Nor standing still");
        } finally {
            player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void threeCellSizesAndTheUpgradeChain(GameTestHelper h) {
        ItemStack plain = new ItemStack(ModItems.PULSE_CELL.get()), greater = new ItemStack(ModItems.GREATER_PULSE_CELL.get()), grand = new ItemStack(ModItems.GRAND_PULSE_CELL.get());
        h.assertTrue(PulseCellItem.capacity(plain) == PulseCellItem.CAPACITY
                        && PulseCellItem.capacity(greater) == PulseCellItem.GREATER_CAPACITY
                        && PulseCellItem.capacity(grand) == PulseCellItem.GRAND_CAPACITY,
                "A cell, a Greater and a Grand each hold their own capacity");
        PulseCellItem.setPulse(grand, PulseCellItem.GRAND_CAPACITY);
        h.assertTrue(PulseCellItem.getPulse(grand) == PulseCellItem.GRAND_CAPACITY, "A Grand cell fills to the brim");
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        PulseCellItem.setPulse(plain, 700);
        h.assertTrue(tk.darrow.tribalpower.item.GearCell.offer(pick, plain) == ItemStack.EMPTY, "The plain cell seats");
        ItemStack back = tk.darrow.tribalpower.item.GearCell.offer(pick, greater);
        h.assertTrue(back.is(ModItems.PULSE_CELL.get()) && PulseCellItem.getPulse(back) == 700, "A Greater cell swaps in and hands the plain one back charged");
        back = tk.darrow.tribalpower.item.GearCell.offer(pick, grand);
        h.assertTrue(back.is(ModItems.GREATER_PULSE_CELL.get()) && tk.darrow.tribalpower.item.GearCell.capacity(pick) == PulseCellItem.GRAND_CAPACITY, "A Grand cell swaps in over a Greater");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void earthLeggingsShedCobwebAndSoulSand(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        var web = h.absolutePos(new BlockPos(4, 2, 4));
        h.getLevel().setBlockAndUpdate(web, Blocks.COBWEB.defaultBlockState());
        player.moveTo(web.getX() + 0.5, web.getY(), web.getZ() + 0.5, 0, 0);
        // Bare legs: the web snares the next move to a quarter.
        Blocks.COBWEB.defaultBlockState().entityInside(h.getLevel(), web, player);
        tk.darrow.tribalpower.item.SpiritGearHooks.shedSnares(player, null);
        double x = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.2, 0, 0));
        h.assertTrue(player.getX() - x < 0.1, "Without the leggings a cobweb still snares, moved " + (player.getX() - x));
        // Earth leggings: the snare is shed after the move, so the next one runs at full length.
        Blocks.COBWEB.defaultBlockState().entityInside(h.getLevel(), web, player);
        tk.darrow.tribalpower.item.SpiritGearHooks.shedSnares(player, Attunement.EARTH);
        x = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.2, 0, 0));
        h.assertTrue(Math.abs(player.getX() - x - 0.2) < 1.0E-4, "Earth leggings push through a cobweb, moved " + (player.getX() - x));

        var sand = h.absolutePos(new BlockPos(2, 1, 2));
        h.getLevel().setBlockAndUpdate(sand, Blocks.SOUL_SAND.defaultBlockState());
        player.moveTo(sand.getX() + 0.5, sand.getY() + 0.875, sand.getZ() + 0.5, 0, 0);
        var efficiency = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_EFFICIENCY);
        tk.darrow.tribalpower.item.SpiritGearHooks.soulStride(player, Attunement.LOOM);
        h.assertTrue(efficiency.getValue() == 0, "Loom leggings do not stride soul sand");
        tk.darrow.tribalpower.item.SpiritGearHooks.soulStride(player, Attunement.EARTH);
        h.assertTrue(efficiency.getValue() == 1, "Earth leggings on soul sand give the synced full movement efficiency");
        player.moveTo(sand.getX() + 0.5, sand.getY() + 3, sand.getZ() + 0.5, 0, 0);
        tk.darrow.tribalpower.item.SpiritGearHooks.soulStride(player, Attunement.EARTH);
        h.assertTrue(efficiency.getValue() == 0, "Off the soul sand the stride is taken back");
        h.succeed();
    }
}
