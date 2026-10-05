package tk.darrow.tribalpower.verification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.TribalPower;

/**
 * The creative tabs are how JEI and EMI order the mod, so every item has to be in one of them, and in only one.
 * A new item that nobody added to a tab fails here instead of turning up loose at the end of the item list.
 */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class CreativeTabGameTests {
    private CreativeTabGameTests() {}

    /** The tabs, in the order they sit after the vanilla ones. */
    private static final List<String> TABS = List.of("main", "gear", "hearth", "march");

    /** Registered on purpose with no tab. Anything added here needs a reason beside it. */
    private static final Set<String> HIDDEN = Set.of(
            // Hoed March soil; it turns back to soil on its own, like vanilla farmland
            "march_farmland",
            // Its metal lives in a component that server tags fill in; see GritItems.displayItems
            "mineral_grit");

    @GameTest(template = "empty")
    public static void everyItemSitsInExactlyOneTab(GameTestHelper h) {
        var level = h.getLevel();
        var params = new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess());
        Map<Item, List<String>> tabsOf = new LinkedHashMap<>();
        List<String> found = new ArrayList<>();
        for (var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            ResourceLocation tabId = entry.getKey().location();
            if (!tabId.getNamespace().equals(TribalPower.MOD_ID)) continue;
            found.add(tabId.getPath());
            CreativeModeTab tab = entry.getValue();
            tab.buildContents(params);
            h.assertFalse(tab.getDisplayItems().isEmpty(), "The " + tabId + " tab has items");
            Set<Item> inThisTab = new HashSet<>();
            for (ItemStack stack : tab.getDisplayItems())
                if (inThisTab.add(stack.getItem())) tabsOf.computeIfAbsent(stack.getItem(), item -> new ArrayList<>()).add(tabId.getPath());
        }
        h.assertTrue(found.size() == TABS.size() && found.containsAll(TABS), "The mod's tabs are " + TABS + ", found " + found);
        // The tabs sit side by side in that order. The sorted list is filled where tabs are sorted; skip where it is empty.
        List<ResourceLocation> sorted = CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .map(tab -> BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)).toList();
        int first = sorted.indexOf(ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, TABS.get(0)));
        if (!sorted.isEmpty()) for (int i = 0; i < TABS.size(); i++)
            h.assertTrue(first >= 0 && first + i < sorted.size()
                    && sorted.get(first + i).equals(ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, TABS.get(i))),
                    "The tabs sit together in order " + TABS + ": " + sorted);

        List<String> missing = new ArrayList<>(), doubled = new ArrayList<>(), shown = new ArrayList<>(), known = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(TribalPower.MOD_ID)) continue;
            List<String> in = tabsOf.getOrDefault(item, List.of());
            if (HIDDEN.contains(id.getPath())) {
                known.add(id.getPath());
                if (!in.isEmpty()) shown.add(id.getPath() + " " + in);
            } else if (in.isEmpty()) missing.add(id.getPath());
            else if (in.size() > 1) doubled.add(id.getPath() + " " + in);
        }
        h.assertTrue(missing.isEmpty(), "Items in no Tribal Power tab: " + missing);
        h.assertTrue(doubled.isEmpty(), "Items in more than one Tribal Power tab: " + doubled);
        h.assertTrue(shown.isEmpty(), "Items on the hidden list that a tab shows: " + shown);
        h.assertTrue(known.size() == HIDDEN.size(), "Every hidden id is a registered item: " + HIDDEN + " vs " + known);
        h.succeed();
    }
}
