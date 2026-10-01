package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.anvil.SpiritAnvil;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.ModEntities;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.kit.KitRegistry;
import tk.darrow.tribalpower.song.PulseBowItem;
import tk.darrow.tribalpower.song.PulseCrossbowItem;
import tk.darrow.tribalpower.song.SonicBolt;
import tk.darrow.tribalpower.song.SongVerse;

/** The Pulse Bow and Crossbow as Spiritgear: what they are made of, how they mend, how they wear and what they hit for. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class RangedGearGameTests {
    private RangedGearGameTests() {}

    private static List<Item> ranged() {
        return List.of(ModItems.PULSE_BOW.get(), KitRegistry.PULSE_CROSSBOW.get());
    }

    @GameTest(template = "empty")
    public static void rangedGearMendsWithManifestedIngots(GameTestHelper h) {
        var level = h.getLevel();
        for (Item item : ranged()) {
            ItemStack fresh = new ItemStack(item);
            String name = BuiltInRegistries.ITEM.getKey(item).getPath();
            h.assertTrue(fresh.getMaxDamage() == SpiritGear.TOOL_DURABILITY, name + " lasts as long as the Spiritgear tools, saw " + fresh.getMaxDamage());
            h.assertTrue(fresh.is(SpiritAnvil.SPIRITGEAR), name + " is Spiritgear to the Spirit Anvil");
            int quarter = fresh.getMaxDamage() / 4;
            int wear = quarter * 3 + 10;
            ItemStack worn = fresh.copy();
            worn.setDamageValue(wear);
            var two = SpiritAnvil.repair(worn, new ItemStack(ModItems.MANIFESTED_INGOT.get(), 2), null);
            h.assertTrue(two != null && two.ingots() == 2 && two.result().is(item) && two.result().getDamageValue() == wear - 2 * quarter,
                    name + ": two Manifested Ingots mend two quarters, saw " + (two == null ? "no mend" : two.ingots() + " ingots, damage " + two.result().getDamageValue()));
            h.assertTrue(SpiritAnvil.repair(worn, new ItemStack(Items.IRON_INGOT), null) == null, name + " does not mend with iron");
            // two worn ones combine in the crafting grid, as any tool does
            ItemStack other = fresh.copy();
            other.setDamageValue(wear);
            var combine = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, List.of(worn, other)), level);
            h.assertTrue(combine.isPresent(), name + ": two worn ones combine in the grid");
            ItemStack combined = combine.get().value().assemble(CraftingInput.of(2, 1, List.of(worn, other)), level.registryAccess());
            h.assertTrue(combined.is(item) && combined.getDamageValue() < wear, name + ": combining mends, saw " + combined.getDamageValue());
        }
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, String... rows) {
        var level = h.getLevel();
        java.util.List<ItemStack> grid = new java.util.ArrayList<>();
        for (String row : rows) {
            for (char c : row.toCharArray()) {
                grid.add(switch (c) {
                    case 'M' -> new ItemStack(ModItems.MANIFESTED_INGOT.get());
                    case 'B' -> new ItemStack(ModItems.BONE_CHIME.get());
                    case 'S' -> new ItemStack(Items.STRING);
                    case 'T' -> new ItemStack(Items.TRIPWIRE_HOOK);
                    case 'K' -> new ItemStack(Items.STICK);
                    case 'C' -> new ItemStack(ModItems.COPPER_RESONATOR.get());
                    default -> ItemStack.EMPTY;
                });
            }
        }
        var input = CraftingInput.of(3, 3, grid);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    @GameTest(template = "empty")
    public static void rangedGearCraftsFromManifestedIngots(GameTestHelper h) {
        var level = h.getLevel();
        ItemStack manifested = new ItemStack(ModItems.MANIFESTED_INGOT.get());
        for (Item item : ranged()) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            var holder = level.getRecipeManager().byKey(id);
            h.assertTrue(holder.isPresent(), id + " has a recipe");
            var ingredients = holder.get().value().getIngredients();
            h.assertTrue(ingredients.stream().filter(i -> i.test(manifested)).count() == 2, id + " takes two Manifested Ingots");
            h.assertFalse(ingredients.stream().anyMatch(i -> i.test(new ItemStack(ModItems.COPPER_RESONATOR.get()))), id + " no longer takes a Copper Resonator");
            h.assertFalse(ingredients.stream().anyMatch(i -> i.test(new ItemStack(ModItems.PULSE_BOW.get()))), id + " no longer takes a Pulse Bow");
        }
        h.assertTrue(craft(h, " MS", "B S", " MS").is(ModItems.PULSE_BOW.get()), "Manifested Ingots, a Bone Chime and string make the Pulse Bow");
        h.assertTrue(craft(h, "MBM", "STS", " K ").is(KitRegistry.PULSE_CROSSBOW.get()), "Manifested Ingots, a Bone Chime, string, a tripwire hook and a stick make the Pulse Crossbow");
        h.assertFalse(craft(h, " SC", "B S", " SC").is(ModItems.PULSE_BOW.get()), "The old resonator grid no longer makes a Pulse Bow");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void boltDamageReadsTheConfig(GameTestHelper h) {
        double full = TribalConfig.bowDamage();
        h.assertTrue(Math.abs(SonicBolt.damage(1.0F, null) - full) < 1.0E-6, "A full draw hits for bowDamage");
        h.assertTrue(Math.abs(SonicBolt.damage(0.5F, null) - full * 0.5) < 1.0E-6, "A half draw hits for half");
        float tap = BowItem.getPowerForTime(3);
        h.assertTrue(tap >= 0.1F && Math.ceil(SonicBolt.damage(tap, null)) <= Math.ceil(full * 0.15),
                "A tapped draw is a tap, not a full bolt: " + SonicBolt.damage(tap, null));
        var strong = new SongVerse(List.of("ember_heart", "ember_heart", "ember_heart", "ember_heart", "ember_heart", "ember_heart", "ember_heart"), Attunement.FIRE);
        var weak = new SongVerse(List.of("ember_heart", "dawn_velvet", "lantern_down"), Attunement.FIRE);
        h.assertTrue(strong.power() == 7 && weak.power() == 1, "Sheet powers are 7 and 1");
        double cap = TribalConfig.verseDamageCap();
        h.assertTrue(Math.abs(SonicBolt.damage(1.0F, strong) - (full + Math.min(cap, 7 * TribalConfig.verseDamagePerPower()))) < 1.0E-6,
                "A strong verse adds no more than the cap: " + SonicBolt.damage(1.0F, strong));
        h.assertTrue(Math.abs(SonicBolt.damage(1.0F, weak) - (full + Math.min(cap, TribalConfig.verseDamagePerPower()))) < 1.0E-6,
                "A weak verse adds its power");
        h.assertTrue(SonicBolt.damage(1.0F, weak) >= SonicBolt.damage(1.0F, null), "A verse never hits softer than a plain bolt");

        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = true;
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            ItemStack bow = new ItemStack(ModItems.PULSE_BOW.get());
            SonicBolt bowBolt = SonicBolt.shoot(player, bow, null, 1.0F);
            double bowHit = bowBolt.getBaseDamage() * bowBolt.getDeltaMovement().length();
            h.assertTrue(Math.abs(bowHit - full) < full * 0.03, "A full-draw bolt leaves hitting for bowDamage, saw " + bowHit);
            bowBolt.discard();

            ItemStack crossbow = new ItemStack(KitRegistry.PULSE_CROSSBOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
            var item = (PulseCrossbowItem) crossbow.getItem();
            item.releaseUsing(crossbow, h.getLevel(), player, item.getUseDuration(crossbow, player) - TribalConfig.crossbowLoadTicks());
            h.assertTrue(PulseCrossbowItem.loaded(crossbow), "The crossbow loads");
            item.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            var bolts = h.getLevel().getEntities(ModEntities.SONIC_BOLT.get(), new AABB(player.blockPosition()).inflate(8), b -> b.isAlive());
            h.assertTrue(bolts.size() == 1, "The crossbow fired one bolt, saw " + bolts.size());
            SonicBolt bolt = bolts.get(0);
            double want = full * TribalConfig.crossbowDamageMultiplier();
            double hit = bolt.getBaseDamage() * bolt.getDeltaMovement().length();
            h.assertTrue(Math.abs(hit - want) < want * 0.03, "A crossbow bolt leaves hitting for bowDamage x crossbowDamageScale, saw " + hit + " want " + want);
            bolt.discard();
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void rangedGearWearsAndSpendsWithUse(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.setGameMode(GameType.SURVIVAL);
            ItemStack cell = PulseCellItem.createFilled(500);
            player.getInventory().setItem(9, cell);
            ItemStack bow = new ItemStack(ModItems.PULSE_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);
            var bowItem = (PulseBowItem) bow.getItem();
            bowItem.releaseUsing(bow, h.getLevel(), player, bowItem.getUseDuration(bow, player) - 20);
            h.assertTrue(bow.getDamageValue() == 1, "A full draw wears the bow a point, saw " + bow.getDamageValue());
            h.assertTrue(PulseCellItem.getPulse(player.getInventory().getItem(9)) == 500 - TribalConfig.bowPulse(),
                    "A full draw spends bowPulse, saw " + PulseCellItem.getPulse(player.getInventory().getItem(9)));

            ItemStack crossbow = new ItemStack(KitRegistry.PULSE_CROSSBOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
            var item = (PulseCrossbowItem) crossbow.getItem();
            item.releaseUsing(crossbow, h.getLevel(), player, item.getUseDuration(crossbow, player) - TribalConfig.crossbowLoadTicks());
            h.assertTrue(PulseCrossbowItem.loaded(crossbow), "The crossbow loads in survival");
            h.assertTrue(PulseCellItem.getPulse(player.getInventory().getItem(9)) == 500 - TribalConfig.bowPulse() - TribalConfig.crossbowPulse(),
                    "Loading spends crossbowPulse");
            item.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            h.assertTrue(crossbow.getDamageValue() == 1, "A shot wears the crossbow a point, saw " + crossbow.getDamageValue());
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /** The Codex quotes the bow and crossbow numbers at the shipped settings; they must follow the config. */
    @GameTest(template = "empty")
    public static void codexQuotesTheRangedNumbers(GameTestHelper h) {
        var book = CodexFiles.english();
        String bow = PulseBowItem.damage(TribalConfig.bowDamage());
        String bolt = PulseBowItem.damage(TribalConfig.bowDamage() * TribalConfig.crossbowDamageMultiplier());
        String verse = PulseBowItem.damage(Math.min(TribalConfig.verseDamageCap(), TribalConfig.verseDamagePerPower() * 7));
        String durability = String.format(java.util.Locale.ROOT, "%,d", SpiritGear.TOOL_DURABILITY);
        String songBench = CodexFacts.words(book.byId().get("song_bench"));
        String crossbow = CodexFacts.words(book.byId().get("pulse_crossbow"));
        String anvil = CodexFacts.words(book.byId().get("spirit_anvil"));
        for (String phrase : List.of("A full draw hits for " + bow, "up to " + verse + ".", "out of " + durability, "Manifested Ingots", "Spirit Anvil",
                "spends up to " + TribalConfig.bowPulse() + " Pulse", "adds " + TribalConfig.versePulse() + " more"))
            h.assertTrue(songBench.contains(phrase), "song_bench must say \"" + phrase + "\"");
        for (String phrase : List.of(bolt + " up close against the bow's " + bow, "up to " + verse + " more to a full bow draw",
                TribalConfig.crossbowPulse() + " Pulse up front", "for " + TribalConfig.versePulse() + " Pulse more", "out of " + durability, "Manifested Ingots"))
            h.assertTrue(crossbow.contains(phrase), "pulse_crossbow must say \"" + phrase + "\"");
        h.assertFalse(crossbow.contains("half again as hard"), "pulse_crossbow must not keep the old one-and-a-half damage");
        h.assertTrue(anvil.contains("Pulse Bow and Pulse Crossbow"), "spirit_anvil must say the bows mend there");
        h.succeed();
    }
}
