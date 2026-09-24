package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.cuisine.Dish;
import tk.darrow.tribalpower.cuisine.FeastBlock;
import tk.darrow.tribalpower.cuisine.HearthPotBlockEntity;
import tk.darrow.tribalpower.cuisine.MarchCrop;
import tk.darrow.tribalpower.cuisine.MarchCropBlock;
import tk.darrow.tribalpower.effect.ModEffects;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;

/** Tribal cuisine: crops that grow and generate, a pot that cooks, dishes that carry boons, feasts that bless. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class CuisineGameTests {
    private CuisineGameTests() {}

    @GameTest(template = "empty")
    public static void cropsGrowAndGenerateWild(GameTestHelper h) {
        var level = (ServerLevel) h.getLevel();
        for (MarchCrop crop : MarchCrop.values()) {
            var placed = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("tribalpower", "wild_" + crop.id()));
            var configured = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("tribalpower", "wild_" + crop.id()));
            h.assertTrue(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(placed), crop + " has a wild patch");
            h.assertTrue(level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(configured), crop + " has a wild feature");
            var biome = level.registryAccess().registryOrThrow(Registries.BIOME).get(ResourceLocation.fromNamespaceAndPath("tribalpower", crop.biome));
            h.assertTrue(biome != null && biome.getGenerationSettings().features().stream()
                    .anyMatch(step -> step.stream().anyMatch(f -> f.unwrapKey().map(k -> k.equals(placed)).orElse(false))), crop + " grows wild in " + crop.biome);
        }
        h.setBlock(2, 1, 2, Blocks.FARMLAND);
        h.setBlock(2, 2, 2, CuisineRegistry.CROPS.get(MarchCrop.EMBERROOT).get());
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var block = CuisineRegistry.CROPS.get(MarchCrop.EMBERROOT).get();
        for (int i = 0; i < 400 && !block.isMaxAge(level.getBlockState(pos)); i++) level.getBlockState(pos).randomTick(level, pos, level.random);
        h.assertTrue(block.isMaxAge(level.getBlockState(pos)), "Emberroot grows to full on farmland");
        h.assertTrue(block.getCloneItemStack(level, pos, level.getBlockState(pos)).is(CuisineRegistry.CROP_ITEMS.get(MarchCrop.EMBERROOT).get()), "The crop is its own seed");
        h.assertTrue(new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.FEN_RICE).get()).is(net.minecraft.tags.ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "crops"))), "Crops carry c:crops");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theHoeReapsAndReplantsMarchCrops(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = true;
            var level = (ServerLevel) h.getLevel();
            h.setBlock(2, 1, 2, Blocks.FARMLAND);
            var block = CuisineRegistry.CROPS.get(MarchCrop.STEPPE_GRAIN).get();
            h.setBlock(2, 2, 2, block.defaultBlockState().setValue(MarchCropBlock.AGE, 3));
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            ItemStack hoe = new ItemStack(ModItems.SPIRITGEAR_HOE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            hoe.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            h.assertTrue(level.getBlockState(pos).is(block) && block.getAge(level.getBlockState(pos)) == 0, "The hoe reaps the grain and sows it again");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void theHearthPotCooksADishThatCarriesItsBoon(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            h.setBlock(2, 2, 2, Blocks.CAMPFIRE);
            h.setBlock(2, 3, 2, CuisineRegistry.HEARTH_POT.get());
            var pot = (HearthPotBlockEntity) h.getBlockEntity(new BlockPos(2, 3, 2));
            var level = (ServerLevel) h.getLevel();
            pot.setItem(0, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.EMBERROOT).get(), 2));
            pot.setItem(1, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.EMBERROOT).get()));
            pot.setItem(2, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.STEPPE_GRAIN).get()));
            h.assertFalse(pot.cookNow(level), "No bowl, no mash");
            pot.setItem(HearthPotBlockEntity.CONTAINER, new ItemStack(Items.BOWL, 2));
            h.assertTrue(pot.cookNow(level), "Emberroot, grain and a bowl make a mash, state " + pot.state());
            ItemStack made = pot.getItem(HearthPotBlockEntity.OUTPUT);
            h.assertTrue(made.is(CuisineRegistry.DISHES.get(Dish.KEEPERS_ROOT_MASH).get()), "It is the Pad-keepers' mash");
            h.assertTrue(pot.getItem(0).getCount() == 1 && pot.getItem(HearthPotBlockEntity.CONTAINER).getCount() == 1, "One of each was spent");
            h.setBlock(2, 2, 2, Blocks.STONE);
            pot.setItem(1, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.EMBERROOT).get()));
            pot.setItem(2, new ItemStack(CuisineRegistry.CROP_ITEMS.get(MarchCrop.STEPPE_GRAIN).get()));
            h.assertFalse(pot.cookNow(level), "No fire, no cooking");
            h.assertTrue(pot.state() == HearthPotBlockEntity.NO_HEAT, "The pot says it needs a fire");
            player.getFoodData().setFoodLevel(2);
            made.getItem().finishUsingItem(made, level, player);
            h.assertTrue(ModEffects.hasBoon(player, TribeDefinition.SOIL), "Eating the mash carries the Pad-keepers' boon");
            h.assertTrue(TribeDefinition.SOIL.offeringValue(new ItemStack(CuisineRegistry.dish(TribeDefinition.SOIL))) == TribalConfig.dishStanding(), "A tribe prizes its own dish");
            h.assertTrue(TribeDefinition.SOIL.trades().stream().anyMatch(o -> o.rank() == TribeRank.FRIEND && o.result().get().is(CuisineRegistry.dish(TribeDefinition.SOIL))),
                    "Kin sell their dish at Friend");
            for (Dish dish : Dish.values()) {
                var recipes = level.getRecipeManager().getAllRecipesFor(CuisineRegistry.HEARTH_TYPE.get());
                h.assertTrue(recipes.stream().anyMatch(r -> r.value().result().is(CuisineRegistry.DISHES.get(dish).get())), dish + " has a hearth recipe");
            }
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void aFeastServesSixAndBlessesEachEater(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            var level = (ServerLevel) h.getLevel();
            h.setBlock(2, 1, 2, Blocks.STONE);
            var feast = CuisineRegistry.FEASTS.get(Attunement.WATER).get();
            h.setBlock(2, 2, 2, feast);
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            player.getFoodData().setFoodLevel(0);
            for (int serving = 0; serving < FeastBlock.SERVINGS; serving++) {
                player.getFoodData().setFoodLevel(0);
                var result = feast.eat(level, pos, level.getBlockState(pos), player);
                h.assertTrue(result.consumesAction(), "Serving " + (serving + 1) + " is eaten");
            }
            h.assertTrue(level.getBlockState(pos).isAir(), "Six servings and the feast is gone");
            h.assertTrue(ModEffects.blessed(player, Attunement.WATER), "The eater is blessed with Water");
            h.assertTrue(level.getRecipeManager().getAllRecipesFor(CuisineRegistry.HEARTH_TYPE.get()).stream()
                    .anyMatch(r -> r.value().result().is(CuisineRegistry.FEAST_ITEMS.get(Attunement.WATER).get())), "The feast is cooked at the hearth");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
