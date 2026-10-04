package tk.darrow.tribalpower.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.entity.BreedingFood;
import tk.darrow.tribalpower.entity.CreatureEntities;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.entity.LatticeAnimal;
import tk.darrow.tribalpower.entity.ModEntities;
import tk.darrow.tribalpower.item.CreatureItems;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.wildlife.WildBreeding;
import tk.darrow.tribalpower.wildlife.Wildlife;

/**
 * Every gentle March creature can be farmed: it eats something a player can carry in, two adults fed it make a
 * young one, and the animals with hide and flesh drop leather and meat when hunted.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class HerdGameTests {
    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(x, 1, z, Blocks.STONE);
    }

    private static Player feeder(GameTestHelper h) {
        // A FakePlayer plays in survival, so food is really spent; sneaking asks a wild animal to breed.
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        var at = h.absolutePos(new BlockPos(1, 2, 1));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        player.setShiftKeyDown(true);
        return player;
    }

    private static void clear(GameTestHelper h) {
        h.getLevel().getEntitiesOfClass(Entity.class, h.getBounds().inflate(4),
                e -> !(e instanceof Player)).forEach(Entity::discard);
    }

    private static List<EntityType<?>> otherGentleLife() {
        return List.of(ModEntities.MARCH_WALKER.get(), ModEntities.SPIRIT_WISP.get(), Wildlife.GLIMMERFIN.get(),
                Wildlife.DRIFT_BELL.get(), Wildlife.VEIL_RAY.get(), Wildlife.LOOM_SWIFT.get());
    }

    /** No gentle creature may be bred only with its own reagent, which only that creature gives. */
    @GameTest(template = "empty")
    public static void everyGentleCreatureEatsSomethingAPlayerCanCarry(GameTestHelper h) {
        for (var p : CreatureProfile.values()) {
            var food = BreedingFood.of(p);
            if (!p.animal) {
                h.assertTrue(food == null, p.id + " is a remnant and keeps its reagent as food");
                continue;
            }
            h.assertTrue(food != null, p.id + " needs a breeding food");
            var sample = food.sample();
            h.assertTrue(!sample.isEmpty() && !sample.is(Items.AIR), p.id + " eats an item that exists: " + food.items());
            h.assertFalse(sample.is(CreatureItems.REAGENTS.get(p).get()), p.id + " must not need its own reagent to breed");
            var animal = CreatureEntities.ANIMALS.get(p).get().create(h.getLevel());
            h.assertTrue(animal.isFood(sample), p.id + " must take " + sample.getItem() + " as food");
        }
        for (var type : otherGentleLife()) {
            var food = BreedingFood.of(type);
            h.assertTrue(food != null && !food.sample().isEmpty(), BuiltInRegistries.ENTITY_TYPE.getKey(type) + " needs a breeding food");
        }
        h.assertTrue(BreedingFood.of(Wildlife.SILT_EEL.get()) == null, "The Silt Eel bites; it is not farmed");
        // The three that always had a food keep it.
        h.assertTrue(BreedingFood.of(CreatureProfile.DAWN_STAG).test(new ItemStack(Items.WHEAT))
                && BreedingFood.of(CreatureProfile.LANTERN_FOX).test(new ItemStack(Items.SWEET_BERRIES))
                && BreedingFood.of(CreatureProfile.MOSSBACK).test(new ItemStack(Items.SEAGRASS)), "Stag wheat, fox berries and Mossback seagrass stay");
        h.succeed();
    }

    /** Two wild adults of every gentle kind, fed while sneaking, fall in love and make a young one of their kind. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void twoFedAdultsOfEveryGentleKindMakeAYoungOne(GameTestHelper h) {
        floor(h);
        var player = feeder(h);
        ServerLevel level = h.getLevel();
        for (var p : CreatureProfile.values()) {
            if (!p.animal) continue;
            var type = CreatureEntities.ANIMALS.get(p).get();
            var a = h.spawn(type, new BlockPos(3, 2, 3));
            var b = h.spawn(type, new BlockPos(4, 2, 3));
            a.setNoAi(true);
            b.setNoAi(true);
            var food = BreedingFood.of(p).sample().copyWithCount(4);
            player.setItemInHand(InteractionHand.MAIN_HAND, food);
            a.mobInteract(player, InteractionHand.MAIN_HAND);
            b.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(a.isInLove() && b.isInLove(), p.id + " must fall in love on " + food.getItem());
            h.assertTrue(food.getCount() == 2, p.id + ": each feed spends one, left " + food.getCount());
            h.assertTrue(!a.isBonded() && !b.isBonded(), p.id + ": sneak-feeding breeds, it does not tame");
            a.spawnChildFromBreeding(level, b);
            var young = level.getEntitiesOfClass(LatticeAnimal.class, h.getBounds().inflate(4), e -> e.getType() == type && e.isBaby());
            h.assertTrue(young.size() == 1, p.id + " must have one young, found " + young.size());
            h.assertTrue(young.get(0).getAge() < 0 && young.get(0).lattice().rolled(), p.id + "'s young must be a growing child with threads");
            clear(h);
        }
        h.succeed();
    }

    /** Walkers and wisps breed like animals; fish, jellies, rays and swifts pair up and make one more of their kind. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theMarchsSmallerLifeBreedsToo(GameTestHelper h) {
        floor(h);
        var player = feeder(h);
        ServerLevel level = h.getLevel();
        for (var type : otherGentleLife()) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            var a = (Mob) h.spawn(type, new BlockPos(3, 2, 3));
            var b = (Mob) h.spawn(type, new BlockPos(4, 2, 3));
            a.setNoAi(true);
            b.setNoAi(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, BreedingFood.of(type).sample().copyWithCount(4));
            a.interact(player, InteractionHand.MAIN_HAND);
            b.interact(player, InteractionHand.MAIN_HAND);
            int before = level.getEntitiesOfClass(Mob.class, h.getBounds().inflate(4), e -> e.getType() == type).size();
            if (a instanceof Animal animal) {
                h.assertTrue(animal.isInLove() && ((Animal) b).isInLove(), id + " must fall in love on its food");
                animal.spawnChildFromBreeding(level, (Animal) b);
                var young = level.getEntitiesOfClass(Animal.class, h.getBounds().inflate(4), e -> e.getType() == type && e.isBaby());
                h.assertTrue(young.size() == 1, id + " must have one young, found " + young.size());
                h.assertTrue(young.get(0).isPersistenceRequired(), id + "'s bred young must not despawn");
                // (h.spawn marks the parents persistent, so only the override itself can be asked here.)
                h.assertTrue(a.removeWhenFarAway(200), id + ": a wild one still despawns as it always did");
            } else {
                var state = ((WildBreeding.Breeder) a).breeding();
                h.assertTrue(state.inLove() && ((WildBreeding.Breeder) b).breeding().inLove(), id + " must fall in love on its food");
                WildBreeding.tick(a, state);
                var all = level.getEntitiesOfClass(Mob.class, h.getBounds().inflate(4), e -> e.getType() == type);
                h.assertTrue(all.size() == before + 1, id + " pair must make one more, " + before + " -> " + all.size());
                // Only the new one: h.spawn already made the parents persistent.
                var young = all.stream().filter(e -> e != a && e != b).toList();
                h.assertTrue(young.size() == 1 && young.get(0).isPersistenceRequired(), id + "'s bred young must not despawn");
                h.assertTrue(!state.inLove() && state.cooldown() > 0, id + " rests after breeding");
            }
            clear(h);
        }
        // The eel is not fed.
        var eel = h.spawn(Wildlife.SILT_EEL.get(), new BlockPos(3, 2, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SEAGRASS, 4));
        eel.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(!eel.breeding().inLove() && player.getMainHandItem().getCount() == 4, "A Silt Eel takes no food");
        clear(h);
        h.succeed();
    }

    /** Bonded pairs still breed by plain feeding, and their young are born into the owner's company with threads. */
    @GameTest(template = "empty")
    public static void bondedPairsStillBreedIntoTheirOwnersCompany(GameTestHelper h) {
        floor(h);
        var player = VerificationPlayers.inLevel(h);
        try {
            var type = CreatureEntities.ANIMALS.get(CreatureProfile.TUFTBACK).get();
            var a = h.spawn(type, new BlockPos(3, 2, 3));
            var b = h.spawn(type, new BlockPos(4, 2, 3));
            a.setNoAi(true);
            b.setNoAi(true);
            a.bond(player);
            b.bond(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, BreedingFood.of(CreatureProfile.TUFTBACK).sample().copyWithCount(4));
            a.mobInteract(player, InteractionHand.MAIN_HAND);
            b.mobInteract(player, InteractionHand.MAIN_HAND);
            h.assertTrue(a.isInLove() && b.isInLove(), "Healthy companions fall in love on their food");
            a.spawnChildFromBreeding(h.getLevel(), b);
            var young = h.getLevel().getEntitiesOfClass(LatticeAnimal.class, h.getBounds().inflate(4), LivingEntity::isBaby);
            h.assertTrue(young.size() == 1, "One young, found " + young.size());
            h.assertTrue(young.get(0).isBonded() && young.get(0).isOwnedBy(player), "The young of two companions joins their owner");
            h.assertTrue(young.get(0).lattice().rolled(), "The young inherits threads");
            clear(h);
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    /** A real herd, left to its own AI: two fed walkers find each other, and the young grows up. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void fedWalkersBreedOnTheirOwnAndTheYoungGrows(GameTestHelper h) {
        floor(h);
        // A closed pen: walkers flee any monster they can see, and the tests next door are full of them.
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            h.setBlock(x, 5, z, Blocks.STONE);
            if (x == 0 || z == 0 || x == 7 || z == 7) for (int y = 2; y < 5; y++) h.setBlock(x, y, z, Blocks.STONE);
        }
        var player = feeder(h);
        var a = h.spawn(ModEntities.MARCH_WALKER.get(), new BlockPos(3, 2, 3));
        var b = h.spawn(ModEntities.MARCH_WALKER.get(), new BlockPos(5, 2, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
        a.mobInteract(player, InteractionHand.MAIN_HAND);
        b.mobInteract(player, InteractionHand.MAIN_HAND);
        // Moved off so the tempt goal does not hold them on the feeder rather than on each other.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // The young is found as "a walker that is neither parent": once grown it is no longer a baby to search for.
        boolean[] bornYoung = {false};
        h.succeedWhen(() -> {
            var young = h.getLevel().getEntitiesOfClass(Animal.class, h.getBounds().inflate(4),
                    e -> e.getType() == ModEntities.MARCH_WALKER.get() && e != a && e != b);
            h.assertTrue(young.size() == 1, "Waiting for the walkers to breed");
            var calf = young.get(0);
            if (calf.isBaby()) bornYoung[0] = true;
            h.assertTrue(bornYoung[0], "A bred walker must be born young");
            // Speed the wait for growing up; the growing itself is the game's own.
            if (calf.getAge() < -2) calf.setAge(-2);
            h.assertTrue(!calf.isBaby(), "Waiting for the young walker to grow up");
        });
    }

    private static List<ItemStack> roll(GameTestHelper h, LivingEntity entity, Player killer, int times) {
        ServerLevel level = h.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(entity.getLootTable());
        var drops = new ArrayList<ItemStack>();
        for (int i = 0; i < times; i++) {
            var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                    .create(LootContextParamSets.ENTITY);
            drops.addAll(table.getRandomItems(params));
        }
        return drops;
    }

    private static void drops(GameTestHelper h, EntityType<? extends LivingEntity> type, Player killer, Item... expected) {
        var entity = h.spawn(type, new BlockPos(3, 2, 3));
        if (entity instanceof Mob mob) mob.setNoAi(true);
        var drops = roll(h, entity, killer, 40);
        for (Item item : expected)
            h.assertTrue(drops.stream().anyMatch(s -> s.is(item)), BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + " must drop " + item);
        entity.discard();
    }

    /** Every creature's loot table loads; the hide-and-flesh ones give leather and meat, the fliers feathers. */
    @GameTest(template = "empty")
    public static void huntedCreaturesDropLeatherMeatAndFeathers(GameTestHelper h) {
        floor(h);
        var killer = feeder(h);
        killer.setShiftKeyDown(false);
        var server = h.getLevel().getServer();
        var types = new ArrayList<EntityType<?>>();
        for (var p : CreatureProfile.values()) types.add(CreatureEntities.type(p));
        types.addAll(otherGentleLife());
        for (var type : types)
            h.assertTrue(server.reloadableRegistries().getLootTable(type.getDefaultLootTable()) != LootTable.EMPTY,
                    BuiltInRegistries.ENTITY_TYPE.getKey(type) + " has no loot table, or it failed to load");
        Item game = ModItems.RAW_GAME.get(), leather = ModItems.MARCH_LEATHER.get();
        drops(h, CreatureEntities.ANIMALS.get(CreatureProfile.TUFTBACK).get(), killer, game, leather, CreatureItems.REAGENTS.get(CreatureProfile.TUFTBACK).get());
        drops(h, CreatureEntities.ANIMALS.get(CreatureProfile.FEN_STRIDER).get(), killer, game, leather, Items.BONE);
        drops(h, CreatureEntities.ANIMALS.get(CreatureProfile.LANTERN_FOX).get(), killer, game, leather);
        drops(h, CreatureEntities.ANIMALS.get(CreatureProfile.RIDGE_GRAZER).get(), killer, game, leather);
        drops(h, ModEntities.MARCH_WALKER.get(), killer, game, leather);
        drops(h, CreatureEntities.ANIMALS.get(CreatureProfile.PALEWING).get(), killer, Items.FEATHER);
        drops(h, CreatureEntities.ANIMALS.get(CreatureProfile.HEARTH_WARDEN).get(), killer, BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tribalpower:hearthoak_log")));
        drops(h, CreatureEntities.MONSTERS.get(CreatureProfile.CRAG_TROLL).get(), killer, leather, Items.BONE, CreatureItems.REAGENTS.get(CreatureProfile.CRAG_TROLL).get());
        drops(h, CreatureEntities.MONSTERS.get(CreatureProfile.RIFT_HOUND).get(), killer, Items.BONE, CreatureItems.REAGENTS.get(CreatureProfile.RIFT_HOUND).get());
        // Like a cow: an animal that dies burning drops its meat roasted.
        var burning = h.spawn(CreatureEntities.ANIMALS.get(CreatureProfile.LONGSHANK).get(), new BlockPos(3, 2, 3));
        burning.setNoAi(true);
        burning.igniteForSeconds(10);
        var roasted = roll(h, burning, killer, 20);
        h.assertTrue(roasted.stream().anyMatch(s -> s.is(ModItems.ROAST_GAME.get())) && roasted.stream().noneMatch(s -> s.is(game)),
                "A burning Longshank drops Roast Game, never raw");
        burning.discard();
        h.succeed();
    }

    /** The real thing: a FakePlayer kills a Cragcoat, and its meat lands on the ground. */
    @GameTest(template = "empty")
    public static void aCragcoatKilledByAPlayerDropsMeat(GameTestHelper h) {
        floor(h);
        var killer = feeder(h);
        killer.setShiftKeyDown(false);
        var cragcoat = h.spawn(CreatureEntities.ANIMALS.get(CreatureProfile.CRAGCOAT).get(), new BlockPos(3, 2, 3));
        cragcoat.setNoAi(true);
        cragcoat.hurt(h.getLevel().damageSources().playerAttack(killer), 1000);
        h.assertTrue(cragcoat.isDeadOrDying(), "The Cragcoat must die");
        var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(4));
        h.assertTrue(items.stream().anyMatch(e -> e.getItem().is(ModItems.RAW_GAME.get())), "A hunted Cragcoat drops Raw Game");
        h.assertTrue(items.stream().anyMatch(e -> e.getItem().is(CreatureItems.REAGENTS.get(CreatureProfile.CRAGCOAT).get())), "and still its Crag Fleece");
        clear(h);
        h.getLevel().getEntitiesOfClass(ExperienceOrb.class, h.getBounds().inflate(4)).forEach(Entity::discard);
        h.succeed();
    }
}
