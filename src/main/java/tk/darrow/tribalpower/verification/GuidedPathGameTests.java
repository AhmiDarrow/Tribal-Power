package tk.darrow.tribalpower.verification;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.guide.NextStep;
import tk.darrow.tribalpower.integration.CompatDisplays;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.quest.QuestStatePayload;

/** The guided path: one spine that exists end to end, a next step that moves as it is walked, and the common tags the pack promises. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class GuidedPathGameTests {
    private GuidedPathGameTests() {}

    @GameTest(template = "empty")
    public static void theSpineExistsAndTheNextStepWalksIt(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            var advancements = player.server.getAdvancements();
            for (String id : NextStep.SPINE)
                h.assertTrue(advancements.get(ResourceLocation.fromNamespaceAndPath("tribalpower", id)) != null, "Spine advancement " + id + " exists");
            h.assertTrue(NextStep.of(player).equals(NextStep.SPINE.get(0)), "A new player starts at the first step, got " + NextStep.of(player));
            h.assertTrue(QuestStatePayload.of(player).nextStep().equals(NextStep.SPINE.get(0)), "The Codex hears the first step");
            earn(player, NextStep.SPINE.get(0));
            h.assertTrue(NextStep.of(player).equals(NextStep.SPINE.get(1)), "Earning the first moves the step on");
            for (String id : NextStep.SPINE) earn(player, id);
            h.assertTrue(NextStep.of(player).equals(NextStep.DONE), "The whole spine earned is done");
            h.assertTrue(NextStep.index(NextStep.DONE) == NextStep.SPINE.size(), "Done is past the last step");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    /** Grants every criterion, whatever the advancement's trigger; code-granted ones have one, the journey's have their own. */
    private static void earn(net.minecraft.server.level.ServerPlayer player, String id) {
        var holder = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("tribalpower", id));
        for (String criterion : holder.value().criteria().keySet()) player.getAdvancements().award(holder, criterion);
    }

    @GameTest(template = "empty")
    public static void commonTagsAndViewerDisplaysAreWhole(GameTestHelper h) {
        var melee = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/melee_weapon"));
        var tools = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools"));
        h.assertTrue(new ItemStack(ModItems.SPIRITGEAR_BLADE.get()).is(melee), "The blade is a melee weapon");
        for (var weapon : ModItems.SPIRITGEAR_WEAPONS.values()) h.assertTrue(new ItemStack(weapon.get()).is(melee) && new ItemStack(weapon.get()).is(tools), "Every Spiritgear weapon is a tool");
        h.assertTrue(new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get()).is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/mining_tool"))), "The pickaxe is a mining tool");
        h.assertTrue(new ItemStack(ModItems.SPIRITGEAR_PICKAXE.get()).is(ItemTags.PICKAXES), "And a vanilla pickaxe");
        h.assertTrue(CompatDisplays.rites().size() == tk.darrow.tribalpower.rite.world.WorldRite.values().length, "Every rite is shown");
        h.assertTrue(CompatDisplays.calls().size() == tk.darrow.tribalpower.guardian.Guardian.values().length, "Every guardian call is shown");
        for (var anoint : CompatDisplays.anointments()) h.assertTrue(!anoint.reagents().isEmpty(), anoint.anointment() + " has reagents to show");
        for (var call : CompatDisplays.calls()) h.assertTrue(BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(call.rises().getItem())), "Eggs exist");
        h.succeed();
    }
}
