package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.client.codex.CodexBook;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.cuisine.Dish;
import tk.darrow.tribalpower.cuisine.Fare;
import tk.darrow.tribalpower.cuisine.HearthPotBlockEntity;
import tk.darrow.tribalpower.cuisine.HearthRecipe;

/** Camp fare: every meal cooks at the pot from its recipe, fills as it says, lends its gift and hands back its bowl. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class FareGameTests {
    private FareGameTests() {}

    @GameTest(template = "empty")
    public static void everyFareCooksAtThePotAndFillsAsItSays(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.setGameMode(GameType.SURVIVAL);
            var level = (ServerLevel) h.getLevel();
            h.setBlock(2, 2, 2, Blocks.CAMPFIRE);
            h.setBlock(2, 3, 2, CuisineRegistry.HEARTH_POT.get());
            var pot = (HearthPotBlockEntity) h.getBlockEntity(new BlockPos(2, 3, 2));
            var recipes = level.getRecipeManager().getAllRecipesFor(CuisineRegistry.HEARTH_TYPE.get());
            for (Fare fare : Fare.values()) {
                var item = CuisineRegistry.FARE.get(fare).get();
                var found = recipes.stream().filter(r -> r.value().result().is(item)).toList();
                h.assertTrue(found.size() == 1, fare + " has one hearth recipe, found " + found.size());
                HearthRecipe recipe = found.getFirst().value();
                for (int i = 0; i < HearthPotBlockEntity.SIZE; i++) pot.setItem(i, ItemStack.EMPTY);
                for (int i = 0; i < recipe.ingredients().size(); i++) {
                    var choices = recipe.ingredients().get(i).getItems();
                    h.assertTrue(choices.length > 0, fare + ": ingredient " + i + " matches nothing (an empty tag?)");
                    pot.setItem(i, new ItemStack(choices[0].getItem()));
                }
                recipe.container().ifPresent(c -> pot.setItem(HearthPotBlockEntity.CONTAINER, new ItemStack(c.getItems()[0].getItem())));
                h.assertTrue(recipe.container().map(c -> fare.serving.container != null && c.test(new ItemStack(fare.serving.container)))
                        .orElse(fare.serving.container == null), fare + " is cooked into what it is served in");
                h.assertTrue(pot.cookNow(level), fare + " cooks, state " + pot.state());
                ItemStack made = pot.getItem(HearthPotBlockEntity.OUTPUT);
                h.assertTrue(made.is(item) && made.getCount() == recipe.result().getCount(), fare + " comes out of the pot, got " + made);
                for (int i = 0; i <= HearthPotBlockEntity.CONTAINER; i++)
                    h.assertTrue(pot.getItem(i).isEmpty(), fare + " uses every seat it was given (seat " + i + " holds " + pot.getItem(i) + ")");
                h.assertTrue(made.getMaxStackSize() == fare.stack, fare + " stacks to " + fare.stack);

                // eaten from empty: the hunger and saturation it promises, its gift and its bowl or bottle back
                player.removeAllEffects();
                player.getFoodData().setFoodLevel(0);
                player.getFoodData().setSaturation(0);
                ItemStack left = made.copyWithCount(1).finishUsingItem(level, player);
                h.assertTrue(player.getFoodData().getFoodLevel() == fare.nutrition, fare + " fills " + fare.nutrition + ", filled " + player.getFoodData().getFoodLevel());
                float saturation = Math.min(fare.nutrition * fare.saturation * 2.0F, fare.nutrition);
                h.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - saturation) < 0.01F, fare + " saturates " + saturation);
                if (fare.effect != null) {
                    MobEffectInstance gift = player.getEffect(fare.effect);
                    h.assertTrue(gift != null && gift.getAmplifier() == 0, fare + " lends its gift at level I");
                    h.assertTrue(gift.getDuration() == TribalConfig.fareEffectSeconds(fare) * 20, fare + " lends it for the configured seconds");
                } else {
                    h.assertTrue(player.getActiveEffects().isEmpty(), fare + " is plain food");
                }
                if (fare.serving.container != null) h.assertTrue(left.is(fare.serving.container), fare + " hands back its " + fare.serving.container + ", got " + left);
                else h.assertTrue(left.isEmpty(), fare + " is eaten whole");
            }
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /** A tribe dish is the best meal in camp: no fare outfills the fullest, so its boon is never the worse buy. */
    @GameTest(template = "empty")
    public static void noFareOutfillsTheTribeDishes(GameTestHelper h) {
        int topNutrition = 0;
        float topSaturation = 0;
        for (Dish dish : Dish.values()) {
            topNutrition = Math.max(topNutrition, dish.nutrition);
            topSaturation = Math.max(topSaturation, dish.nutrition * dish.saturation);
        }
        for (Fare fare : Fare.values()) {
            h.assertTrue(fare.nutrition <= topNutrition, fare + " fills more than any tribe dish");
            if (fare.has(Fare.Trait.HEARTY)) {
                // hearty fare trades the gift away for the longest fullness in camp, and still fills less
                h.assertTrue(fare.effect == null && fare.nutrition < topNutrition, fare + " is hearty but carries a gift or fills as much as a dish");
                h.assertTrue(fare.nutrition * fare.saturation > topSaturation, fare + " is hearty but saturates no better than a dish");
                continue;
            }
            h.assertTrue(fare.nutrition * fare.saturation < topSaturation, fare + " saturates as much as the best tribe dish");
            h.assertTrue(fare.effect == null || TribalConfig.fareEffectSeconds(fare) <= 120, fare + " lends a long gift; keep fare's gifts short");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void thistleTeaSettlesAStomachAndCrispsAreQuick(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.setGameMode(GameType.SURVIVAL);
            var level = (ServerLevel) h.getLevel();
            player.getFoodData().setFoodLevel(20);
            ItemStack tea = new ItemStack(CuisineRegistry.FARE.get(Fare.THISTLE_TEA).get());
            ItemStack bread = new ItemStack(CuisineRegistry.FARE.get(Fare.DROVERS_FLATBREAD).get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bread);
            h.assertFalse(bread.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Flatbread waits for hunger");
            player.stopUsingItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, tea);
            h.assertTrue(tea.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Tea can be drunk on a full stomach");
            player.stopUsingItem();
            h.assertTrue(tea.getUseAnimation() == net.minecraft.world.item.UseAnim.DRINK, "Tea is drunk, not eaten");
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
            tea.finishUsingItem(level, player);
            h.assertTrue(!player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.HUNGER) && !player.hasEffect(MobEffects.CONFUSION),
                    "Thistle Tea cures poison, hunger and nausea");
            var crisps = new ItemStack(CuisineRegistry.FARE.get(Fare.GLIMMER_CRISPS).get()).get(DataComponents.FOOD);
            var skewer = new ItemStack(CuisineRegistry.FARE.get(Fare.HUNTERS_SKEWER).get()).get(DataComponents.FOOD);
            h.assertTrue(crisps != null && skewer != null && crisps.eatDurationTicks() < skewer.eatDurationTicks(), "Crisps are eaten faster than a meal");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /** The Codex states each gift's length; it has to be the length the game gives. */
    @GameTest(template = "empty")
    public static void theCodexNamesEveryFareAndItsGift(GameTestHelper h) {
        var book = CodexFiles.english();
        for (Fare fare : Fare.values()) {
            String id = "tribalpower:" + fare.id();
            var entry = book.entries().stream().filter(e -> e.items().contains(id)).findFirst().orElse(null);
            h.assertTrue(entry != null && entry.category().equals("cuisine"), fare + " is listed in a Hearth & Table entry");
            h.assertTrue(entry.pages().stream().anyMatch(p -> p instanceof CodexBook.Recipe r && r.item().equals(id)), fare + " has its recipe page");
            if (fare.effect == null) continue;
            String text = String.join(" ", entry.pages().stream().map(CodexBook.Page::text).toList());
            String says = "for " + TribalConfig.fareEffectSeconds(fare) + " seconds";
            h.assertTrue(text.contains(says), entry.id() + " should say " + fare + " lasts '" + says + "'");
        }
        h.succeed();
    }
}
