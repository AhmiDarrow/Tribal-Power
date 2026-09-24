package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity;
import tk.darrow.tribalpower.blockentity.RitualBrazierBlockEntity;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.CreatureEntities;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.healing.HealingCircles;
import tk.darrow.tribalpower.healing.HealingHooks;
import tk.darrow.tribalpower.healing.HealingRegistry;
import tk.darrow.tribalpower.healing.Remedies;
import tk.darrow.tribalpower.healing.Remedy;
import tk.darrow.tribalpower.healing.SpiritKettleBlockEntity;
import tk.darrow.tribalpower.healing.SpiritRemnantItem;
import tk.darrow.tribalpower.healing.SweatStonesBlock;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.item.SpiritgearRattleItem;
import tk.darrow.tribalpower.song.Reagents;

/** Shamanic healing: the kettle, remedies, Spirit Sickness, the rattle, remnants, incense, the circle and the lodge. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class HealingGameTests {
    private HealingGameTests() {}

    /** The mock player joins at world spawn; tests that reach it by area bring it into the structure. */
    private static void standAt(GameTestHelper h, net.minecraft.server.level.ServerPlayer player, int x, int y, int z) {
        var at = h.absolutePos(new BlockPos(x, y, z));
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    @GameTest(template = "empty")
    public static void kettleBrewsOverHeat(GameTestHelper h) {
        h.setBlock(2, 2, 2, Blocks.CAMPFIRE);
        h.setBlock(2, 3, 2, HealingRegistry.SPIRIT_KETTLE.get());
        h.setBlock(4, 2, 2, ModBlocks.RESONANCE_TOTEM_WATER.get());
        h.setBlock(2, 2, 4, ModBlocks.DRUMHEART.get());
        ((DrumheartBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 4))).insertPulse(400, false);
        var kettle = (SpiritKettleBlockEntity) h.getBlockEntity(new BlockPos(2, 3, 2));
        kettle.setItem(SpiritKettleBlockEntity.REAGENT, new ItemStack(Reagents.item(CreatureProfile.ASHBOUND), 2));
        kettle.setItem(SpiritKettleBlockEntity.HERB, new ItemStack(ModItems.SPIRIT_REED.get(), 2));
        kettle.setItem(SpiritKettleBlockEntity.BASE, new ItemStack(Items.GLASS_BOTTLE, 2));
        var level = (ServerLevel) h.getLevel();
        h.assertTrue(kettle.brewNow(level), "A heated kettle with all three inputs brews, state " + kettle.state());
        ItemStack made = kettle.getItem(SpiritKettleBlockEntity.OUTPUT);
        h.assertTrue(made.is(HealingRegistry.SPIRIT_TINCTURE.get()) && made.getCount() == TribalConfig.tinctureYield(), "A bottle makes tinctures");
        h.assertTrue(Remedies.remedy(made) == Remedy.KINDLING, "An ember reagent brews Kindling");
        h.assertTrue(Remedies.voice(made) == Attunement.WATER, "The water totem sang into it");
        h.assertTrue(kettle.getItem(SpiritKettleBlockEntity.REAGENT).getCount() == 1, "One reagent was spent");

        h.setBlock(2, 2, 2, Blocks.STONE);
        h.assertFalse(kettle.brewNow(level), "No heat, no brew");
        h.assertTrue(kettle.state() == SpiritKettleBlockEntity.NO_HEAT, "The kettle says it needs heat");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sicknessStacksAndRemediesLiftIt(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            float healthy = player.getMaxHealth();
            h.assertTrue(HealingHooks.sicken(player), "A plain player can fall sick");
            HealingHooks.sicken(player);
            var sickness = player.getEffect(HealingRegistry.SPIRIT_SICKNESS);
            h.assertTrue(sickness != null && sickness.getAmplifier() == 1, "Sickness stacks");
            h.assertTrue(player.getMaxHealth() < healthy, "Sickness costs max health, " + player.getMaxHealth());
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 200));
            ItemStack salve = Remedies.make(Remedies.Form.SALVE, CreatureProfile.DAWN_STAG, Attunement.SPIRIT, 1);
            player.setHealth(4);
            Remedies.apply(player, salve);
            h.assertFalse(player.hasEffect(HealingRegistry.SPIRIT_SICKNESS), "A salve lifts the sickness");
            h.assertFalse(player.hasEffect(MobEffects.POISON), "A salve lifts every harm");
            h.assertTrue(player.getHealth() > 4, "A salve heals at once");
            h.assertTrue(player.hasEffect(MobEffects.REGENERATION), "Mending grants regeneration");
            h.assertTrue(Math.abs(player.getMaxHealth() - healthy) < 0.01, "Health comes back when the sickness lifts");

            HealingHooks.bless(player);
            h.assertTrue(player.hasEffect(HealingRegistry.SPIRIT_BLESSING), "The lodge blesses");
            h.assertFalse(HealingHooks.sicken(player), "A blessed player cannot fall sick");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void rattleMendsForPulse(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            ItemStack rattle = new ItemStack(ModItems.SPIRITGEAR_RATTLE.get());
            h.assertTrue(SpiritGear.isTool(rattle) && SpiritGear.rankFormula("echo_attune", rattle) != null, "The rattle ranks like Spiritgear");
            player.setHealth(6);
            SpiritgearRattleItem.shake(player, rattle, player);
            h.assertTrue(player.getHealth() > 6, "A shake heals, health " + player.getHealth());
            HealingHooks.sicken(player);
            HealingHooks.sicken(player);
            SpiritGear.setRank(rattle, 2);
            SpiritgearRattleItem.shake(player, rattle, player);
            var sickness = player.getEffect(HealingRegistry.SPIRIT_SICKNESS);
            h.assertTrue(sickness != null && sickness.getAmplifier() == 0, "A Bound rattle eases the sickness a level");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void incenseBurnsForEveryoneNear(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            h.setBlock(2, 2, 2, ModBlocks.RITUAL_BRAZIER.get());
            standAt(h, player, 3, 2, 3);
            var brazier = (RitualBrazierBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 2));
            ItemStack incense = Remedies.make(Remedies.Form.INCENSE, CreatureProfile.ROOTBOUND, null, 1);
            h.assertTrue(brazier.addIncense(incense) == 1, "The brazier takes incense");
            h.assertTrue(brazier.addIncense(Remedies.make(Remedies.Form.INCENSE, CreatureProfile.ASHBOUND, null, 1)) == 0,
                    "One brew at a time");
            var level = (ServerLevel) h.getLevel();
            brazier.burnIncense(level);
            h.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Rooting incense reaches a player nearby");
            for (int i = 1; i < TribalConfig.incenseBeats(); i++) brazier.burnIncense(level);
            h.assertTrue(brazier.incense().isEmpty(), "A stick burns out after its beats");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void theCircleHealsAndCallsTheFallenBack(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            var level = (ServerLevel) h.getLevel();
            var hound = CreatureEntities.MONSTERS.get(CreatureProfile.RIFT_HOUND).get().create(level);
            var at = h.absolutePos(new BlockPos(1, 2, 1));
            hound.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            hound.setNoAi(true);
            level.addFreshEntity(hound);
            hound.bond(player);
            ItemStack remnant = SpiritRemnantItem.of(hound);
            hound.discard();

            BlockPos brazier = new BlockPos(3, 2, 3);
            h.setBlock(brazier, ModBlocks.RITUAL_BRAZIER.get());
            h.setBlock(brazier.offset(2, 0, 2), ModBlocks.RITE_PEDESTAL.get());
            var pedestal = (RitePedestalBlockEntity) h.getBlockEntity(brazier.offset(2, 0, 2));
            pedestal.setItem(RitePedestalBlockEntity.SLOT, remnant);
            standAt(h, player, 4, 2, 4);
            player.setHealth(3);
            int revived = HealingCircles.cast(level, h.absolutePos(brazier), 400);
            h.assertTrue(revived == 1, "The remnant on the pedestal is called back, got " + revived);
            h.assertTrue(pedestal.held().isEmpty(), "The pedestal gives the remnant up");
            var back = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(brazier)).inflate(6),
                    e -> e instanceof Familiar familiar && familiar.isBonded() && familiar.isOwnedBy(player));
            h.assertTrue(back.size() == 1 && back.get(0).getHealth() == back.get(0).getMaxHealth(), "The familiar returns whole and bonded");
            h.assertTrue(player.getHealth() > 3 && player.hasEffect(HealingRegistry.SPIRIT_BLESSING), "The circle heals and blesses");
            h.assertTrue(HealingCircles.open(level) >= 1, "The circle stays open");
            back.forEach(LivingEntity::discard);
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void aLodgeNeedsHotStonesAndARoof(GameTestHelper h) {
        var level = (ServerLevel) h.getLevel();
        BlockPos bed = new BlockPos(2, 2, 2);
        h.setBlock(4, 1, 2, Blocks.CAMPFIRE);
        h.setBlock(4, 2, 2, HealingRegistry.SWEAT_STONES.get());
        h.assertTrue(h.getBlockState(new BlockPos(4, 2, 2)).getValue(SweatStonesBlock.HOT), "Stones over a fire are hot");
        if (TribalConfig.lodgeNeedsRoof()) h.assertFalse(HealingHooks.lodge(level, h.absolutePos(bed)), "No roof, no lodge");
        h.setBlock(2, 5, 2, Blocks.OAK_PLANKS);
        h.assertTrue(HealingHooks.lodge(level, h.absolutePos(bed)), "Hot stones and a roof make a lodge");
        h.setBlock(4, 1, 2, Blocks.STONE);
        h.assertFalse(h.getBlockState(new BlockPos(4, 2, 2)).getValue(SweatStonesBlock.HOT), "Stones cool when the fire goes");
        h.succeed();
    }
}
