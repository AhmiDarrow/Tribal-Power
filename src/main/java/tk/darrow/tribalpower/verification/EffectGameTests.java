package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.AfflictionEffect;
import tk.darrow.tribalpower.effect.ModEffects;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.healing.HealingRegistry;
import tk.darrow.tribalpower.healing.Remedies;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.song.SongVerse;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeStanding;

/** The spirit layer: every blessing, boon and affliction applies, ticks and cleanses. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class EffectGameTests {
    private EffectGameTests() {}

    @GameTest(template = "empty")
    public static void everyEffectRegistersWithANameAndIcon(GameTestHelper h) {
        for (var voice : Attunement.values()) h.assertTrue(ModEffects.blessing(voice).isBound(), voice + " blessing registers");
        for (var tribe : TribeDefinition.values()) h.assertTrue(ModEffects.boon(tribe).isBound(), tribe + " boon registers");
        for (var kind : AfflictionEffect.Kind.values()) h.assertTrue(ModEffects.affliction(kind).isBound(), kind + " registers");
        var assets = java.nio.file.Path.of("src/main/resources/assets/tribalpower/textures/mob_effect");
        var all = new java.util.ArrayList<>(ModEffects.EFFECTS.getEntries());
        all.addAll(tk.darrow.tribalpower.healing.HealingRegistry.EFFECTS.getEntries());
        for (var holder : all) {
            String id = holder.getId().getPath();
            h.assertTrue(net.minecraft.locale.Language.getInstance().has("effect.tribalpower." + id), id + " has a name");
            if (java.nio.file.Files.isDirectory(assets))
                h.assertTrue(java.nio.file.Files.isRegularFile(assets.resolve(id + ".png")), id + " has an icon");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void blessingsStackAndRefreshAndMilkLiftsThem(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            ModEffects.bless(player, Attunement.EARTH, 200, 0, false);
            double resist = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            h.assertTrue(Math.abs(resist - TribalConfig.earthBlessingKnockback()) < 1e-6, "Earth stands firm, " + resist);
            ModEffects.bless(player, Attunement.EARTH, 100, 1, false);
            h.assertTrue(ModEffects.blessingLevel(player, Attunement.EARTH) == 2, "A stronger blessing replaces a weaker one");
            ModEffects.bless(player, Attunement.EARTH, 50, 0, false);
            h.assertTrue(ModEffects.blessingLevel(player, Attunement.EARTH) == 2, "A weaker one does not undo it");
            ModEffects.bless(player, Attunement.AIR, 200, 0, false);
            h.assertTrue(player.getAttributeValue(Attributes.JUMP_STRENGTH) > 0.42, "Air jumps higher");
            player.removeAllEffects();
            h.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)) < 1e-6, "Lifting the blessing lifts its firmness");
            ModEffects.bless(player, Attunement.WATER, 200, 0, false);
            player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
            h.assertFalse(ModEffects.blessed(player, Attunement.WATER), "Milk lifts a blessing");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void standingCarriesABoonAndBoonsAct(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            TribeStanding.add(player, TribeDefinition.SPINDLE, TribeRank.KIN.threshold());
            h.assertTrue(TribeStanding.rank(player, TribeDefinition.SPINDLE).ordinal() >= TribeRank.KIN.ordinal(), "Kin with the Loom-stitchers");
            // The tick hands boons out every hundred ticks; hand it out the same way here.
            ModEffects.grantBoon(player, TribeDefinition.SPINDLE, 140, true);
            var verse = new SongVerse(java.util.List.of(CreatureProfile.ASHBOUND.reagent, CreatureProfile.DAWN_STAG.reagent, CreatureProfile.DAWN_STAG.reagent), Attunement.FIRE);
            h.assertTrue(SongVerse.castCost(player, verse) < verse.castPulse(), "A Loom-stitcher sings cheaper");
            ModEffects.grantBoon(player, TribeDefinition.SPARK, 140, true);
            h.assertTrue(player.getAttributeValue(Attributes.ATTACK_SPEED) > 4.0, "A Drumheart swings faster");
            player.removeAllEffects();
            h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 4.0) < 1e-6, "The quicker hand goes with the boon");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void afflictionsBiteAndCleansingLiftsThem(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            float healthy = player.getMaxHealth();
            ModEffects.afflict(player, AfflictionEffect.Kind.FRAYED, 200, 0);
            h.assertTrue(player.getMaxHealth() < healthy, "Frayed takes health, " + player.getMaxHealth());
            player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
            h.assertTrue(ModEffects.afflicted(player, AfflictionEffect.Kind.FRAYED), "Milk does nothing for Frayed");
            ModEffects.afflict(player, AfflictionEffect.Kind.UNSUNG_HUSH, 200, 0);
            h.assertTrue(ModEffects.hushed(player), "Hushed players cannot sing");
            player.addEffect(new MobEffectInstance(HealingRegistry.SPIRIT_SICKNESS, 200, 0));
            ItemStack salve = Remedies.make(Remedies.Form.SALVE, CreatureProfile.DAWN_STAG, null, 1);
            Remedies.apply(player, salve);
            h.assertFalse(ModEffects.afflicted(player, AfflictionEffect.Kind.FRAYED), "A salve mends what was frayed");
            h.assertFalse(ModEffects.afflicted(player, AfflictionEffect.Kind.UNSUNG_HUSH), "A salve gives the voice back");
            h.assertFalse(player.hasEffect(HealingRegistry.SPIRIT_SICKNESS), "A salve lifts the sickness too");
            h.assertTrue(Math.abs(player.getMaxHealth() - healthy) < 0.01, "Health comes back whole");
            ModEffects.afflict(player, AfflictionEffect.Kind.LEY_SICKNESS, 200, 0);
            h.assertTrue(ModEffects.cleanse(player) == 1, "Cleansing counts what it lifts");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void loomTornCreaturesFrayAndTheUnsungHushes(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            var weaver = tk.darrow.tribalpower.entity.CreatureEntities.MONSTERS.get(CreatureProfile.ECHO_WEAVER).get().create(h.getLevel());
            var at = h.absolutePos(new BlockPos(2, 2, 2));
            weaver.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            weaver.setNoAi(true);
            h.getLevel().addFreshEntity(weaver);
            player.teleportTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5);
            player.setInvulnerable(false);
            player.getAbilities().invulnerable = false;
            player.getAbilities().instabuild = false;
            player.invulnerableTime = 0;
            // The test server may sit at Peaceful, where a mob's blow does nothing; the voice is what is under test.
            weaver.applyVoice(player);
            h.assertTrue(ModEffects.afflicted(player, AfflictionEffect.Kind.FRAYED), "An Echo Weaver's touch frays");
            weaver.discard();
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void loomBlessingThreadsPulseBack(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = false;
            ItemStack cell = new ItemStack(ModItems.PULSE_CELL.get());
            tk.darrow.tribalpower.item.PulseCellItem.insertPulse(cell, 100, false);
            player.getInventory().setItem(0, cell);
            ItemStack blade = new ItemStack(ModItems.SPIRITGEAR_BLADE.get());
            h.assertTrue(tk.darrow.tribalpower.item.GearCell.spend(player, blade, 20), "A plain spend works");
            int plain = tk.darrow.tribalpower.item.PulseCellItem.getPulse(cell);
            ModEffects.bless(player, Attunement.LOOM, 200, 0, false);
            h.assertTrue(tk.darrow.tribalpower.item.GearCell.spend(player, blade, 20), "A blessed spend works");
            int blessed = tk.darrow.tribalpower.item.PulseCellItem.getPulse(cell);
            h.assertTrue(plain - blessed < 20, "Loom threads some back: spent " + (plain - blessed));
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
