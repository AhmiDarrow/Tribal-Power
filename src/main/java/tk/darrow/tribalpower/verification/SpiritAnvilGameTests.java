package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.anvil.SpiritAnvil;
import tk.darrow.tribalpower.anvil.SpiritAnvilMenu;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.item.ModItems;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class SpiritAnvilGameTests {
    private static ItemStack worn(int damage) {
        ItemStack pick = new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get());
        pick.setDamageValue(damage);
        return pick;
    }

    @GameTest(template = "empty")
    public static void manifestedIngotsMendAQuarterEach(GameTestHelper h) {
        int quarter = worn(0).getMaxDamage() / 4;
        int wear = quarter * 3 + 10;
        var two = SpiritAnvil.repair(worn(wear), new ItemStack(ModItems.MANIFESTED_INGOT.get(), 2), null);
        h.assertTrue(two != null && two.ingots() == 2 && two.result().getDamageValue() == wear - 2 * quarter && two.cost() == 2,
                "Two ingots mend two quarters for two levels: " + (two == null ? "no mend" : two.ingots() + " ingots, damage " + two.result().getDamageValue() + " of " + two.result().getMaxDamage() + ", cost " + two.cost()));
        var plenty = SpiritAnvil.repair(worn(wear), new ItemStack(ModItems.MANIFESTED_INGOT.get(), 9), null);
        h.assertTrue(plenty != null && plenty.ingots() == 4 && plenty.result().getDamageValue() == 0,
                "Only the ingots the mend needs are taken");
        h.assertTrue(SpiritAnvil.repair(worn(0), new ItemStack(ModItems.MANIFESTED_INGOT.get()), null) == null, "Nothing to mend on unworn gear");
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        iron.setDamageValue(100);
        h.assertTrue(SpiritAnvil.repair(iron, new ItemStack(ModItems.MANIFESTED_INGOT.get()), null) == null, "Only Spiritgear takes Manifested Ingots");
        h.assertTrue(SpiritAnvil.repair(worn(800), new ItemStack(Items.IRON_INGOT), null) == null, "Spiritgear mends with Manifested Ingots, not iron");
        var named = SpiritAnvil.repair(worn(800), new ItemStack(ModItems.MANIFESTED_INGOT.get()), "Old Faithful");
        h.assertTrue(named != null && named.cost() == 2 && named.result().getHoverName().getString().equals("Old Faithful"),
                "A rename on the same go costs a level more, as at any anvil");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theAnvilMendsInItsMenuAndNeverChips(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, ModBlocks.SPIRIT_ANVIL.get());
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.giveExperienceLevels(200);
        BlockPos at = h.absolutePos(pos);
        player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
        var level = h.getLevel();
        for (int go = 0; go < 60; go++) {
            player.openMenu(level.getBlockState(at).getMenuProvider(level, at));
            h.assertTrue(player.containerMenu instanceof SpiritAnvilMenu, "The Spirit Anvil opens its own anvil menu");
            var menu = (SpiritAnvilMenu) player.containerMenu;
            menu.getSlot(0).set(worn(800));
            menu.getSlot(1).set(new ItemStack(ModItems.MANIFESTED_INGOT.get(), 2));
            ItemStack out = menu.getSlot(2).getItem();
            h.assertTrue(out.is(ModItems.SPIRITGEAR_PICKAXE.get()) && out.getDamageValue() == 800 - 2 * (out.getMaxDamage() / 4), "The menu offers the mended tool, go " + go + ": " + out + " damage " + out.getDamageValue());
            h.assertTrue(menu.getSlot(2).mayPickup(player), "The mend can be taken");
            menu.getSlot(2).onTake(player, out);
            h.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "The tool and both ingots are spent");
            player.closeContainer();
            h.assertTrue(level.getBlockState(at).is(ModBlocks.SPIRIT_ANVIL.get()), "The Spirit Anvil never chips, go " + go);
        }
        h.succeed();
    }
}
