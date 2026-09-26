package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.song.ReagentPouch;
import tk.darrow.tribalpower.storage.DeepCacheContainer;
import tk.darrow.tribalpower.storage.DeepCacheManager;
import tk.darrow.tribalpower.storage.DeepCacheSavedData;

/**
 * A Reagent Pouch keeps what it holds wherever it is put (a chest, a shulker box, the Wayfarer vault) through a save and
 * a load; and a Wayfarer Satchel cannot be locked inside the vault it opens.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class PouchStorageGameTests {
    private PouchStorageGameTests() {}

    private static ItemStack fullPouch(CreatureProfile profile) {
        ItemStack pouch = new ItemStack(ModItems.REAGENT_POUCH.get());
        ReagentPouch.addRaw(pouch, profile, 37);
        ReagentPouch.addEmpowered(pouch, profile, 12);
        return pouch;
    }

    private static boolean intact(ItemStack pouch, CreatureProfile profile) {
        return pouch.is(ModItems.REAGENT_POUCH.get()) && ReagentPouch.raw(pouch, profile) == 37 && ReagentPouch.empowered(pouch, profile) == 12;
    }

    @GameTest(template = "empty")
    public static void aPouchKeepsItsReagentsInOtherContainers(GameTestHelper h) {
        CreatureProfile profile = CreatureProfile.values()[0];
        var registries = h.getLevel().registryAccess();

        // a chest, saved and loaded as a chunk would
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, Blocks.CHEST);
        var chest = (ChestBlockEntity) h.getBlockEntity(pos);
        chest.setItem(4, fullPouch(profile));
        CompoundTag tag = chest.saveWithFullMetadata(registries);
        var reloaded = new ChestBlockEntity(h.absolutePos(pos), h.getBlockState(pos));
        reloaded.loadWithComponents(tag, registries);
        h.assertTrue(intact(reloaded.getItem(4), profile), "In a chest the pouch keeps 37 raw and 12 empowered");

        // a shulker box item, carried around with the pouch inside it
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(fullPouch(profile))));
        var saved = shulker.save(registries);
        ItemStack back = ItemStack.parse(registries, saved).orElseThrow();
        ItemStack inside = back.get(DataComponents.CONTAINER).copyOne();
        h.assertTrue(intact(inside, profile), "In a shulker box the pouch keeps its reagents");

        // the Wayfarer vault, through its own saved data
        var player = VerificationPlayers.inLevel(h);
        try {
            DeepCacheContainer vault = DeepCacheManager.openContainer(player);
            vault.setItem(7, fullPouch(profile));
            CompoundTag data = DeepCacheManager.data(player.server).save(new CompoundTag(), registries);
            DeepCacheSavedData loaded = DeepCacheSavedData.load(data, registries);
            DeepCacheContainer again = new DeepCacheContainer(loaded, player.getUUID());
            h.assertTrue(intact(again.getItem(7), profile), "In the Wayfarer vault the pouch keeps its reagents, got " + again.getItem(7));
            vault.setItem(7, ItemStack.EMPTY);
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aSatchelCannotGoIntoTheVaultItOpens(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            DeepCacheContainer vault = DeepCacheManager.openContainer(player);
            ChestMenu menu = DeepCacheContainer.guard(ChestMenu.sixRows(1, player.getInventory(), vault), vault);
            ItemStack satchel = new ItemStack(ModItems.WAYFARER_SATCHEL.get());
            h.assertFalse(menu.getSlot(0).mayPlace(satchel), "A vault slot refuses a Wayfarer Satchel");
            h.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ModItems.REAGENT_POUCH.get())), "but takes a pouch");
            h.assertFalse(vault.canPlaceItem(3, satchel), "and the vault itself refuses one");
            // shift-clicking the satchel from the pack leaves it in the pack
            int packSlot = 54;    // the first slot after the vault's 54 is the player's pack
            menu.getSlot(packSlot).set(satchel);
            menu.quickMoveStack(player, packSlot);
            boolean inVault = false;
            for (int i = 0; i < vault.getContainerSize(); i++) inVault |= vault.getItem(i).is(ModItems.WAYFARER_SATCHEL.get());
            h.assertFalse(inVault, "A shift-click does not slip the satchel into the vault");
            h.assertTrue(menu.getSlot(packSlot).getItem().is(ModItems.WAYFARER_SATCHEL.get()), "It stays in the pack");
            menu.getSlot(packSlot).set(ItemStack.EMPTY);
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
