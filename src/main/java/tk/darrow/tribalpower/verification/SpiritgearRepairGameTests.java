package tk.darrow.tribalpower.verification;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.anvil.SpiritAnvil;
import tk.darrow.tribalpower.item.ModItems;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class SpiritgearRepairGameTests {
    /** Every piece of Spiritgear, bows and Maul included, mends with Manifested Ingots on any anvil and never with diamonds. */
    @GameTest(template = "empty")
    public static void everySpiritgearPieceMendsWithIngotsAlone(GameTestHelper h) {
        ItemStack ingot = new ItemStack(ModItems.MANIFESTED_INGOT.get());
        int pieces = 0;
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(SpiritAnvil.SPIRITGEAR)) {
            ItemStack gear = new ItemStack(holder.value());
            String name = BuiltInRegistries.ITEM.getKey(holder.value()).toString();
            h.assertTrue(gear.getItem().isValidRepairItem(gear, ingot), name + " mends with Manifested Ingots on an anvil");
            h.assertFalse(gear.getItem().isValidRepairItem(gear, new ItemStack(Items.DIAMOND)), name + " does not mend with diamonds");
            gear.setDamageValue(gear.getMaxDamage() / 2);
            var mend = SpiritAnvil.repair(gear, ingot.copyWithCount(1), null);
            h.assertTrue(mend != null && mend.result().getDamageValue() < gear.getDamageValue(), name + " mends at the Spirit Anvil");
            pieces++;
        }
        h.assertTrue(pieces >= 18, "The Spiritgear tag holds every piece, found " + pieces);
        h.assertTrue(new ItemStack(ModItems.PULSE_BOW.get()).is(SpiritAnvil.SPIRITGEAR), "The Pulse Bow is Spiritgear");
        h.succeed();
    }
}
