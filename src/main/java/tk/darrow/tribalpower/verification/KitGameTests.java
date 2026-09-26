package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.block.SpiritDoorBlock;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.MarchThreat;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.RitualChalkItem;
import tk.darrow.tribalpower.item.WeaponKind;
import tk.darrow.tribalpower.kit.KitRegistry;
import tk.darrow.tribalpower.kit.SoulUrnItem;
import tk.darrow.tribalpower.kit.VineLifts;
import tk.darrow.tribalpower.song.PulseCrossbowItem;

/** The camp kit: the Spirit Door, Vine Lifts, Soul Urns, dyes, the Pulse Crossbow, the Songkeeper Drum, weapon weight. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class KitGameTests {
    private KitGameTests() {}

    @GameTest(template = "empty")
    public static void spiritDoorBarsOnlyTheHostile(GameTestHelper h) {
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        var cow = h.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        var player = VerificationPlayers.inLevel(h);
        try {
            h.assertTrue(SpiritDoorBlock.barred(zombie), "A zombie meets a wall");
            h.assertFalse(SpiritDoorBlock.barred(cow), "A cow walks through");
            h.assertFalse(SpiritDoorBlock.barred(player), "A player walks through");
            h.setBlock(2, 2, 2, ModBlocks.SPIRIT_DOOR.get());
            var state = h.getBlockState(new BlockPos(2, 2, 2));
            var level = h.getLevel();
            var at = h.absolutePos(new BlockPos(2, 2, 2));
            h.assertTrue(state.getCollisionShape(level, at, net.minecraft.world.phys.shapes.CollisionContext.of(zombie)).bounds().getXsize() >= 0.99,
                    "The opening is closed to the zombie");
            h.assertTrue(state.getCollisionShape(level, at, net.minecraft.world.phys.shapes.CollisionContext.of(cow)).bounds().getXsize() < 0.99
                    || state.getCollisionShape(level, at, net.minecraft.world.phys.shapes.CollisionContext.of(cow)).bounds().getZsize() < 0.99,
                    "The opening is open to the cow");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void vineLiftsFindEachOther(GameTestHelper h) {
        h.setBlock(2, 1, 2, KitRegistry.VINE_LIFT.get());
        h.setBlock(2, 5, 2, KitRegistry.VINE_LIFT.get());
        var level = h.getLevel();
        var low = h.absolutePos(new BlockPos(2, 1, 2));
        var high = h.absolutePos(new BlockPos(2, 5, 2));
        h.assertTrue(high.equals(VineLifts.next(level, low, 1)), "Jumping finds the lift above");
        h.assertTrue(low.equals(VineLifts.next(level, high, -1)), "Sneaking finds the lift below");
        h.setBlock(2, 6, 2, Blocks.STONE);
        h.assertTrue(VineLifts.next(level, low, 1) == null, "A lift with no room to stand on is passed over");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void soulUrnsTakeAndGiveBack(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = false;
            var level = (ServerLevel) h.getLevel();
            Cow cow = h.spawn(EntityType.COW, new BlockPos(2, 2, 2));
            ItemStack woven = new ItemStack(KitRegistry.SOUL_URNS.get(SoulUrnItem.Tier.WOVEN).get());
            var urn = (SoulUrnItem) woven.getItem();
            h.assertTrue(urn.capture(player, woven, cow), "A woven urn takes a cow");
            h.assertTrue(SoulUrnItem.full(woven) && cow.isRemoved(), "The cow is inside");
            h.assertTrue(urn.release(player, woven, level, h.absolutePos(new BlockPos(3, 2, 3))), "It comes back out");
            h.assertTrue(woven.isEmpty(), "Two uses, and the woven urn is spent");
            h.assertTrue(level.getEntitiesOfClass(Cow.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3, 2, 3))).inflate(2)).size() == 1,
                    "The cow stands where it was set down");

            ItemStack copper = new ItemStack(KitRegistry.SOUL_URNS.get(SoulUrnItem.Tier.COPPER).get());
            var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
            var health = zombie.getAttribute(Attributes.MAX_HEALTH);
            health.addPermanentModifier(new AttributeModifier(MarchThreat.ELITE, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            h.assertFalse(((SoulUrnItem) copper.getItem()).capture(player, copper, zombie), "An elite will not be held");
            h.assertTrue(SoulUrnItem.refusal(player) != null, "A player cannot be held");
            h.assertTrue(SoulUrnItem.refusal(EntityType.WITHER.create(level)) != null, "A boss cannot be held");
            health.removeModifier(MarchThreat.ELITE);
            h.assertTrue(((SoulUrnItem) copper.getItem()).capture(player, copper, zombie), "A plain zombie can");
            h.assertTrue(((SoulUrnItem) copper.getItem()).usesLeft(copper) == TribalConfig.copperUrnUses() - 1, "Capture is one use");
            ItemStack resonant = new ItemStack(KitRegistry.SOUL_URNS.get(SoulUrnItem.Tier.RESONANT).get());
            h.assertTrue(((SoulUrnItem) resonant.getItem()).usesLeft(resonant) < 0, "A resonant urn never wears out");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void reagentsGrindIntoDye(GameTestHelper h) {
        var level = h.getLevel();
        var input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.BOWL), new ItemStack(ModItems.RITUAL_CHALK.get()),
                new ItemStack(tk.darrow.tribalpower.song.Reagents.item(tk.darrow.tribalpower.song.Reagents.byId("pale_down")))));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        h.assertTrue(recipe.isPresent(), "Pale down, chalk and a bowl make a recipe");
        ItemStack dye = recipe.get().value().assemble(input, level.registryAccess());
        h.assertTrue(dye.is(Items.WHITE_DYE) && dye.getCount() == TribalConfig.dyeYield(), "It makes white dye");
        var left = recipe.get().value().getRemainingItems(input);
        h.assertTrue(left.get(0).is(Items.BOWL), "The bowl comes back");
        h.assertTrue(left.get(1).getItem() instanceof RitualChalkItem && RitualChalkItem.remaining(left.get(1)) == RitualChalkItem.USES - 1,
                "The chalk loses one mark");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void crossbowLoadsThenFires(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            player.getAbilities().instabuild = true;
            ItemStack crossbow = new ItemStack(KitRegistry.PULSE_CROSSBOW.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, crossbow);
            var item = (PulseCrossbowItem) crossbow.getItem();
            int duration = item.getUseDuration(crossbow, player);
            h.assertTrue(duration == TribalConfig.crossbowLoadTicks() + 3 && item.useOnRelease(crossbow), "The load ends on its own once complete, as the game's crossbow does");
            item.releaseUsing(crossbow, h.getLevel(), player, duration - TribalConfig.crossbowLoadTicks() + 3);
            h.assertFalse(PulseCrossbowItem.loaded(crossbow), "Let go too soon and it does not load");
            item.releaseUsing(crossbow, h.getLevel(), player, duration - TribalConfig.crossbowLoadTicks());
            h.assertTrue(PulseCrossbowItem.loaded(crossbow), "Held long enough, it loads");
            item.use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertFalse(PulseCrossbowItem.loaded(crossbow), "Using a loaded crossbow fires it");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    // The Songkeeper Drum's tests live in SongkeeperGameTests.

    @GameTest(template = "empty")
    public static void heavyWeaponsSlowAndDaggersQuicken(GameTestHelper h) {
        double hammer = 0, dagger = 0;
        for (var entry : new ItemStack(ModItems.SPIRITGEAR_WEAPONS.get(WeaponKind.WARHAMMER).get())
                .getAttributeModifiers().modifiers()) if (entry.attribute().equals(Attributes.MOVEMENT_SPEED)) hammer += entry.modifier().amount();
        for (var entry : new ItemStack(ModItems.SPIRITGEAR_WEAPONS.get(WeaponKind.DAGGER).get())
                .getAttributeModifiers().modifiers()) if (entry.attribute().equals(Attributes.MOVEMENT_SPEED)) dagger += entry.modifier().amount();
        h.assertTrue(hammer < 0 && dagger > 0, "A warhammer weighs you down and a dagger lightens you: " + hammer + ", " + dagger);
        h.succeed();
    }
}
