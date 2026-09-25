package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.camp.CampBlockEntity;
import tk.darrow.tribalpower.camp.CampRegistry;
import tk.darrow.tribalpower.compat.AgriCraftCompat;

import java.util.UUID;

/**
 * The Grove Tender with AgriCraft in the pack. Passes trivially without it: there is nothing to be compatible
 * with, and the verification run does not always carry the jar.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class AgriCraftGameTests {
    private AgriCraftGameTests() {}

    @GameTest(template = "empty")
    public static void theGroveTenderWorksAgriCraftCropsAsItsOwn(GameTestHelper h) {
        if (!AgriCraftCompat.LOADED) { h.succeed(); return; }
        var level = (ServerLevel) h.getLevel();
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) h.setBlock(x, 1, z, Blocks.FARMLAND);
        h.setBlock(4, 1, 4, Blocks.STONE);
        h.setBlock(4, 2, 4, CampRegistry.DEVICES.get("grove_tender").get());
        h.setBlock(4, 5, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var tender = (CampBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        tender.setOwner(UUID.randomUUID());
        tender.pulse = 2400;
        var sticks = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("agricraft", "wooden_crop_sticks")), 4);
        h.assertTrue(tender.canPlaceItem(0, sticks), "The tender's top row takes crop sticks");
        tender.setItem(0, sticks);
        BlockPos crop = null;
        for (int i = 0; i < 12 && crop == null; i++) {
            tender.work(level);
            for (int x = 1; x <= 7 && crop == null; x++) for (int z = 1; z <= 7; z++) {
                BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
                if (AgriCraftCompat.isCrop(level, at)) { crop = at; break; }
            }
        }
        h.assertTrue(crop != null, "The tender sets crop sticks from its store onto the farmland");
        h.assertTrue(tender.getItem(0).getCount() == 3, "One bundle of sticks was used");
        ItemStack seed = AgriCraftCompat.seedFor(ResourceLocation.fromNamespaceAndPath("minecraft", "wheat"));
        h.assertFalse(seed.isEmpty(), "AgriCraft knows wheat");
        h.assertTrue(tender.canPlaceItem(0, seed), "The tender's top row takes AgriCraft seeds");
        h.assertTrue(AgriCraftCompat.describeSeed(seed).contains("known"), "The seed's plant is known here: " + AgriCraftCompat.describeSeed(seed));
        tender.setItem(0, seed.copy());
        for (int i = 0; i < 12 && !AgriCraftCompat.hasPlant(level, crop); i++) tender.work(level);
        h.assertTrue(AgriCraftCompat.hasPlant(level, crop), "The tender plants the seed into the empty sticks; seat 0 holds " + tender.getItem(0)
                + ", sticks " + AgriCraftCompat.isCrop(level, crop) + ", at " + h.getLevel().getBlockState(crop));
        h.assertTrue(tender.getItem(0).isEmpty(), "The seed was used");
        h.assertTrue(AgriCraftCompat.ripen(level, crop), "The plant can be ripened for the test");
        int before = tender.getItem(9).getCount();
        boolean reaped = false;
        for (int i = 0; i < 12 && !reaped; i++) {
            tender.work(level);
            for (int slot = 9; slot < 27; slot++) if (tender.getItem(slot).is(Items.WHEAT)) reaped = true;
        }
        h.assertTrue(reaped, "The tender reaps ripe AgriCraft wheat into its store");
        h.assertTrue(AgriCraftCompat.hasPlant(level, crop), "The plant stays on its sticks after the harvest");
        h.assertTrue(AgriCraftCompat.harvestable(level, crop) == null, "And is cut back, not ripe again at once");
        h.succeed();
    }

    /** The March crops are AgriCraft plants of their own: the tender seeds sticks with the plain crop item and reaps the same. */
    @GameTest(template = "empty")
    public static void theMarchCropsGrowOnCropSticks(GameTestHelper h) {
        if (!AgriCraftCompat.LOADED) { h.succeed(); return; }
        var level = (ServerLevel) h.getLevel();
        for (var crop : tk.darrow.tribalpower.cuisine.MarchCrop.values()) {
            ItemStack seed = AgriCraftCompat.seedFor(ResourceLocation.fromNamespaceAndPath("tribalpower", crop.id()));
            h.assertFalse(seed.isEmpty(), "AgriCraft knows " + crop.id());
            h.assertTrue(AgriCraftCompat.describeSeed(seed).contains("known"), crop.id() + " is a plant of this world: " + AgriCraftCompat.describeSeed(seed));
        }
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) h.setBlock(x, 1, z, Blocks.FARMLAND);
        h.setBlock(4, 1, 4, Blocks.STONE);
        h.setBlock(4, 2, 4, CampRegistry.DEVICES.get("grove_tender").get());
        h.setBlock(4, 5, 4, ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var tender = (CampBlockEntity) h.getBlockEntity(new BlockPos(4, 2, 4));
        tender.setOwner(UUID.randomUUID());
        tender.pulse = 2400;
        BlockPos crop = h.absolutePos(new BlockPos(2, 2, 2));
        h.assertTrue(AgriCraftCompat.setSticks(level, crop, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("agricraft", "wooden_crop_sticks")))), "Sticks set by hand");
        var emberroot = tk.darrow.tribalpower.cuisine.CuisineRegistry.CROP_ITEMS.get(tk.darrow.tribalpower.cuisine.MarchCrop.EMBERROOT).get();
        tender.setItem(0, new ItemStack(emberroot, 2));
        for (int i = 0; i < 12 && !AgriCraftCompat.hasPlant(level, crop); i++) tender.work(level);
        h.assertTrue(AgriCraftCompat.hasPlant(level, crop), "The tender seeds the sticks with a plain Emberroot; seat 0 holds " + tender.getItem(0));
        h.assertTrue(tender.getItem(0).getCount() == 1, "One Emberroot was used");
        h.assertTrue(AgriCraftCompat.ripen(level, crop), "The Emberroot plant ripens");
        boolean reaped = false;
        for (int i = 0; i < 12 && !reaped; i++) {
            tender.work(level);
            for (int slot = 9; slot < 27; slot++) if (tender.getItem(slot).is(emberroot)) reaped = true;
        }
        h.assertTrue(reaped, "The tender reaps Emberroot from its AgriCraft plant");
        h.assertTrue(AgriCraftCompat.hasPlant(level, crop), "The plant stays on its sticks");
        h.succeed();
    }

    /**
     * AgriCraft renamed its datapack registries: the 4.0.3 build this mod compiles against reads agricraft/plant, soil and
     * mutation; the 4.0.17 release packs ship reads agricraft/plants, soils and mutations. 5.0.0 shipped only the singular
     * set, so a pack on 4.0.17 never saw the March crops. Both sets ship now. This runs without AgriCraft.
     */
    @GameTest(template = "empty")
    public static void theMarchCropsSitWhereEveryAgriCraftReadsThem(GameTestHelper h) {
        var resources = h.getLevel().getServer().getResourceManager();
        java.util.function.Function<String, Long> count = dir -> resources.listResources(dir, id -> id.getPath().endsWith(".json"))
                .keySet().stream().filter(id -> id.getNamespace().equals("tribalpower") && id.getPath().startsWith(dir + "/")).count();
        String[][] expect = {{"agricraft/plant", "agricraft/plants", "5"}, {"agricraft/soil", "agricraft/soils", "3"}, {"agricraft/mutation", "agricraft/mutations", "5"}};
        for (String[] e : expect) {
            long n = Long.parseLong(e[2]);
            h.assertTrue(count.apply(e[0]) == n && count.apply(e[1]) == n,
                    e[0] + " and " + e[1] + " each hold " + n + ", got " + count.apply(e[0]) + " and " + count.apply(e[1]));
        }
        // AgriCraft 4.0.17 will not parse a plant (and so will not start the server) without these on every seed
        for (var entry : resources.listResources("agricraft/plants", id -> id.getNamespace().equals("tribalpower") && id.getPath().endsWith(".json")).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var plant = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                h.assertTrue(plant.getAsJsonObject("requirement").has("seasons"), entry.getKey() + " lists its seasons");
                for (var seed : plant.getAsJsonArray("seeds")) {
                    for (String key : new String[]{"item", "override_planting", "seed_drop_chance", "seed_drop_bonus", "grass_drop_chance"})
                        h.assertTrue(seed.getAsJsonObject().has(key), entry.getKey() + " seed has " + key);
                }
            } catch (java.io.IOException ex) {
                h.fail("Could not read " + entry.getKey() + ": " + ex);
            }
        }
        h.succeed();
    }
}
