package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.DrumheartBlockEntity;
import tk.darrow.tribalpower.blockentity.SongBenchBlockEntity;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.item.WeaponKind;
import tk.darrow.tribalpower.song.Anointing;
import tk.darrow.tribalpower.song.Anointment;
import tk.darrow.tribalpower.song.ReagentPouch;
import tk.darrow.tribalpower.song.SongBenchLogic;

/** The Spiritgear weapon family and anointing at the Song Bench. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class WeaponGameTests {
    private WeaponGameTests() {}

    private static double sum(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        double[] total = {0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (held, modifier) -> {
            if (held.equals(attribute)) total[0] += modifier.amount();
        });
        return total[0];
    }

    @GameTest(template = "empty")
    public static void weaponsCarryTheirConfiguredNumbers(GameTestHelper h) {
        for (WeaponKind kind : WeaponKind.values()) {
            ItemStack stack = new ItemStack(ModItems.SPIRITGEAR_WEAPONS.get(kind).get());
            double damage = sum(stack, Attributes.ATTACK_DAMAGE) + 1;
            double speed = sum(stack, Attributes.ATTACK_SPEED) + 4;
            h.assertTrue(Math.abs(damage - TribalConfig.weaponDamage(kind)) < 1.0E-6, kind + " damage " + damage);
            h.assertTrue(Math.abs(speed - TribalConfig.weaponSpeed(kind)) < 1.0E-6, kind + " speed " + speed);
            h.assertTrue(Math.abs(sum(stack, Attributes.ENTITY_INTERACTION_RANGE) - TribalConfig.weaponReach(kind)) < 1.0E-6, kind + " reach");
            h.assertTrue(stack.getMaxDamage() >= SpiritGear.TOOL_DURABILITY, kind + " durability");
        }
        ItemStack spear = new ItemStack(ModItems.SPIRITGEAR_WEAPONS.get(WeaponKind.SPEAR).get());
        ItemStack dagger = new ItemStack(ModItems.SPIRITGEAR_WEAPONS.get(WeaponKind.DAGGER).get());
        h.assertTrue(sum(spear, Attributes.ENTITY_INTERACTION_RANGE) > 0 && sum(dagger, Attributes.ENTITY_INTERACTION_RANGE) < 0,
                "A spear reaches further than a dagger");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void weaponsRankLikeTheBlade(GameTestHelper h) {
        for (var weapon : ModItems.SPIRITGEAR_WEAPONS.values()) {
            ItemStack stack = new ItemStack(weapon.get());
            h.assertTrue(SpiritGear.isTool(stack), weapon.getId() + " is Spiritgear");
            h.assertTrue(SpiritGear.rankFormula("echo_attune", stack) != null, weapon.getId() + " attunes");
            double plain = sum(stack, Attributes.ATTACK_DAMAGE);
            SpiritGear.setRank(stack, 3);
            h.assertTrue(sum(stack, Attributes.ATTACK_DAMAGE) > plain, weapon.getId() + " hits harder Manifested");
            h.assertTrue(Anointing.canAnoint(stack), weapon.getId() + " takes an anointment");
        }
        h.assertTrue(Anointing.canAnoint(new ItemStack(Items.IRON_SWORD)) && Anointing.canAnoint(new ItemStack(Items.DIAMOND_AXE)),
                "Vanilla swords and axes take anointments too");
        h.assertFalse(Anointing.canAnoint(new ItemStack(Items.STICK)), "A stick does not");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oneAnointmentAtATime(GameTestHelper h) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        Anointing.anoint(sword, CreatureProfile.RIFT_HOUND);
        h.assertTrue(Anointing.anointment(sword).orElse(null) == Anointment.BLOODTHIRST, "Rift tooth is life steal");
        Anointing.anoint(sword, CreatureProfile.ASHBOUND);
        h.assertTrue(Anointing.anointment(sword).orElse(null) == Anointment.SEARING, "A new reagent replaces the old");
        h.assertTrue(Anointing.reagent(sword).orElse(null) == CreatureProfile.ASHBOUND, "Only the newest reagent is kept");
        for (Anointment anointment : Anointment.values())
            for (var param : anointment.params)
                h.assertTrue(TribalConfig.anointing(anointment, param.key()) == param.value(), anointment + "." + param.key() + " reads its config");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bloodthirstStealsLife(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            Anointing.anoint(sword, CreatureProfile.RIFT_HOUND);
            player.setItemSlot(EquipmentSlot.MAINHAND, sword);
            player.setHealth(10);
            var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
            zombie.hurt(player.damageSources().playerAttack(player), 10);
            h.assertTrue(player.getHealth() > 10, "Life steal healed the wielder, health " + player.getHealth());
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void benchAnointsAndReplaces(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.SONG_BENCH.get());
        h.setBlock(4, 2, 2, ModBlocks.RESONANCE_TOTEM_FIRE.get());
        h.setBlock(2, 2, 4, ModBlocks.DRUMHEART.get());
        var bench = (SongBenchBlockEntity) h.getBlockEntity(pos);
        var drum = (DrumheartBlockEntity) h.getBlockEntity(new BlockPos(2, 2, 4));
        drum.insertPulse(400, false);
        var level = (ServerLevel) h.getLevel();
        var origin = bench.getBlockPos();
        ItemStack pouch = new ItemStack(ModItems.REAGENT_POUCH.get());
        int cost = TribalConfig.anointReagentCost();
        ReagentPouch.addEmpowered(pouch, CreatureProfile.RIFT_HOUND, cost);
        ReagentPouch.addEmpowered(pouch, CreatureProfile.ASHBOUND, cost - 1);

        var none = SongBenchLogic.anoint(level, origin, bench, pouch, CreatureProfile.RIFT_HOUND, bench.getItem(SongBenchBlockEntity.WEAPON));
        h.assertFalse(none.ok(), "Nothing to anoint without a weapon");

        Item spear = ModItems.SPIRITGEAR_WEAPONS.get(WeaponKind.SPEAR).get();
        h.assertTrue(bench.canPlaceItem(SongBenchBlockEntity.WEAPON, new ItemStack(spear)), "The weapon seat takes a spear");
        h.assertFalse(bench.canPlaceItem(SongBenchBlockEntity.WEAPON, new ItemStack(Items.PAPER)), "The weapon seat refuses paper");
        bench.setItem(SongBenchBlockEntity.WEAPON, new ItemStack(spear));
        long before = drum.getPulseStored();
        var done = SongBenchLogic.anoint(level, origin, bench, pouch, CreatureProfile.RIFT_HOUND, bench.getItem(SongBenchBlockEntity.WEAPON));
        h.assertTrue(done.ok(), done.message().getString());
        h.assertTrue(Anointing.anointment(bench.getItem(SongBenchBlockEntity.WEAPON)).orElse(null) == Anointment.BLOODTHIRST, "The spear drinks");
        h.assertTrue(ReagentPouch.empowered(pouch, CreatureProfile.RIFT_HOUND) == 0, "The reagents were spent");
        h.assertTrue(drum.getPulseStored() < before, "Pulse was spent");

        var again = SongBenchLogic.anoint(level, origin, bench, pouch, CreatureProfile.RIFT_HOUND, bench.getItem(SongBenchBlockEntity.WEAPON));
        h.assertFalse(again.ok(), "The same anointment twice is refused");
        var short_ = SongBenchLogic.anoint(level, origin, bench, pouch, CreatureProfile.ASHBOUND, bench.getItem(SongBenchBlockEntity.WEAPON));
        h.assertFalse(short_.ok(), "Too few empowered reagents is refused");
        h.assertTrue(ReagentPouch.empowered(pouch, CreatureProfile.ASHBOUND) == cost - 1, "A refusal spends nothing");
        ReagentPouch.addEmpowered(pouch, CreatureProfile.ASHBOUND, 1);
        var swap = SongBenchLogic.anoint(level, origin, bench, pouch, CreatureProfile.ASHBOUND, bench.getItem(SongBenchBlockEntity.WEAPON));
        h.assertTrue(swap.ok(), swap.message().getString());
        h.assertTrue(Anointing.anointment(bench.getItem(SongBenchBlockEntity.WEAPON)).orElse(null) == Anointment.SEARING,
                "The new anointment replaced the old one");
        h.succeed();
    }
}
