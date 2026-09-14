package tk.darrow.tribalpower.verification;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class MenuRegistrationGameTests {
    @GameTest(template = "empty")
    public static void menusExistBeforeAnyScreenLoads(GameTestHelper h) {
        // Inspect the registry without touching a menu class: loading one used to
        // mask registration-order bugs in server verification runs.
        for (String name : new String[]{"echo_station", "relay", "cache", "charm_slots", "mossback_saddlebag"}) {
            h.assertTrue(BuiltInRegistries.MENU.containsKey(ResourceLocation.fromNamespaceAndPath("tribalpower", name)),
                    "Menu must be registered on both client and server: " + name);
        }
        h.succeed();
    }
}
