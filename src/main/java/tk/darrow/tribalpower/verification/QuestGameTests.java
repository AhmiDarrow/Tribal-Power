package tk.darrow.tribalpower.verification;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.quest.Dialogue;
import tk.darrow.tribalpower.quest.DialogueSession;
import tk.darrow.tribalpower.quest.QuestEvents;
import tk.darrow.tribalpower.quest.QuestRegistry;
import tk.darrow.tribalpower.quest.QuestSavedData;
import tk.darrow.tribalpower.quest.QuestStatePayload;
import tk.darrow.tribalpower.quest.Questline;
import tk.darrow.tribalpower.quest.Requests;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeHearthBlockEntity;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeRegistry;
import tk.darrow.tribalpower.tribe.TribeStanding;

/** Tribes that talk: dialogue trees and their conditions, requests, questlines, relics, and what survives a save. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class QuestGameTests {
    private QuestGameTests() {}

    @GameTest(template = "empty")
    public static void everyTribeShipsADialogueTree(GameTestHelper h) {
        for (TribeDefinition tribe : TribeDefinition.values()) {
            Dialogue.Tree tree = Dialogue.tree(tribe);
            h.assertTrue(tree != null, tribe.id() + " has no dialogue");
            h.assertTrue(!tree.roots().isEmpty(), tribe.id() + " dialogue has no roots");
            for (Dialogue.Root root : tree.roots()) h.assertTrue(tree.node(root.node()) != null, tribe.id() + " root " + root.node() + " names no node");
            for (Dialogue.Node node : tree.nodes().values())
                for (Dialogue.Choice choice : node.choices())
                    h.assertTrue(choice.next().isEmpty() || tree.node(choice.next()) != null, tribe.id() + " choice " + choice.text() + " leads nowhere");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dialogueConditionsFollowStandingAndStory(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            TribeDefinition tribe = TribeDefinition.CLOCK;
            var stranger = List.of(new Dialogue.Condition("below_rank", "guest"));
            var guest = List.of(new Dialogue.Condition("rank", "guest"));
            h.assertTrue(DialogueSession.holds(player, tribe, stranger), "A new player is a stranger");
            h.assertFalse(DialogueSession.holds(player, tribe, guest), "A new player is not a guest");
            TribeStanding.add(player, tribe, TribeRank.GUEST.threshold());
            h.assertFalse(DialogueSession.holds(player, tribe, stranger), "Standing lifts the stranger gate");
            h.assertTrue(DialogueSession.holds(player, tribe, guest), "Guest gate opens at Guest");
            h.assertTrue(DialogueSession.holds(player, tribe, List.of(new Dialogue.Condition("quest_step", "0"), new Dialogue.Condition("request", "none"))),
                    "Story starts at step 0 with no request");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.CLOCK));
            h.assertTrue(DialogueSession.holds(player, tribe, List.of(new Dialogue.Condition("holding", "minecraft:clock"))), "Holding condition reads the main hand");
            // A tree parsed from JSON keeps its gates and actions.
            Dialogue.Tree tree = Dialogue.parse(Map.of(ResourceLocation.fromNamespaceAndPath("tribalpower", "clock/test"), JsonParser.parseString(
                    "{\"roots\":[{\"node\":\"a\",\"conditions\":[{\"rank\":\"kin\"}]}],\"nodes\":{\"a\":{\"text\":[\"x\"],\"choices\":[{\"text\":\"y\",\"actions\":[{\"standing\":5}]}]}}}"))).get(tribe);
            h.assertTrue(tree.roots().size() == 1 && tree.roots().get(0).conditions().get(0).kind().equals("rank"), "Root condition parsed");
            h.assertTrue(tree.node("a").choices().get(0).actions().get(0).value().getAsInt() == 5, "Action value parsed");
        } finally {
            player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void requestsPayStandingAndEveryThirdPaysAMark(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            TribeDefinition tribe = TribeDefinition.SOIL;
            QuestSavedData data = QuestSavedData.get(player.server);
            long day = Requests.day(h.getLevel());
            h.assertTrue(Requests.openToday(tribe, day).size() == Requests.OPEN_PER_DAY, "Three requests open a day");
            h.assertTrue(!Requests.openToday(tribe, day).equals(Requests.openToday(tribe, day + 1)), "Tomorrow's requests differ");
            Requests.Template moss = Requests.template(tribe, "soil.moss");
            h.assertTrue(moss != null && moss.kind() == Requests.Kind.FETCH, "The Pad-keepers ask for moss");
            int marks = 0;
            for (int round = 1; round <= 3; round++) {
                data.setRequest(player.getUUID(), tribe, new QuestSavedData.Request(moss.id(), day, 0));
                h.assertFalse(Requests.ready(player, tribe), "Empty-handed is not ready");
                player.getInventory().add(new ItemStack(Items.MOSS_BLOCK, moss.count()));
                h.assertTrue(Requests.ready(player, tribe), "Carrying the moss is ready");
                int before = TribeStanding.get(player.server, player.getUUID(), tribe);
                h.assertTrue(Requests.turnIn(player, tribe), "Turn-in succeeds");
                h.assertTrue(TribeStanding.get(player.server, player.getUUID(), tribe) - before == moss.standing(), "Turn-in pays the request's standing");
                h.assertTrue(player.getInventory().countItem(Items.MOSS_BLOCK) == 0, "Turn-in takes the moss");
                h.assertTrue(data.request(player.getUUID(), tribe) == null, "Turn-in closes the request");
                h.assertTrue(data.completed(player.getUUID(), tribe) == round, "Completed count grows");
                marks = player.getInventory().countItem(TribeRegistry.TRIBE_MARK.get());
                if (round < 3) h.assertTrue(marks == 0, "No mark before the third request");
            }
            h.assertTrue(marks == 1, "The third finished request pays a Tribe Mark");
            // Offering picks one of today's open requests and holds it until it is done.
            Requests.Template offered = Requests.offer(player, tribe);
            h.assertTrue(offered != null && Requests.openToday(tribe, day).stream().anyMatch(t -> t.id().equals(offered.id())), "The Elder offers one of today's requests");
            h.assertTrue(Requests.offer(player, tribe).id().equals(offered.id()), "Asking again repeats the open request");
        } finally {
            player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void questlineRunsItsStepsAndSurvivesASave(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            TribeDefinition tribe = TribeDefinition.SOIL;
            QuestSavedData data = QuestSavedData.get(player.server);
            h.assertTrue(Questline.steps(tribe).size() == Questline.STEPS, "Seven steps");
            h.assertTrue(Questline.ready(player, tribe) && Questline.handIn(player, tribe), "Talking is the first step");
            h.assertTrue(data.step(player.getUUID(), tribe) == 1, "Step 1 after talking");
            TribeStanding.add(player, tribe, TribeRank.FRIEND.threshold());
            h.assertTrue(data.step(player.getUUID(), tribe) == 2, "Reaching Friend advances the rank step on its own");
            h.assertFalse(Questline.ready(player, tribe), "Fetch needs the moss");
            player.getInventory().add(new ItemStack(Items.MOSS_BLOCK, 16));
            h.assertTrue(Questline.handIn(player, tribe), "Sixteen moss hands in");
            h.assertTrue(data.step(player.getUUID(), tribe) == 3 && player.getInventory().countItem(Items.MOSS_BLOCK) == 0, "Fetch taken, step 3");
            // Slaying counts only near the tribe's own hearth.
            BlockPos rel = new BlockPos(2, 2, 2);
            h.setBlock(rel, TribeRegistry.TRIBE_HEARTH.get());
            var hearth = (TribeHearthBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
            hearth.setTribe(tribe);
            var zombie = EntityType.ZOMBIE.create(h.getLevel());
            zombie.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 3)));
            for (int i = 0; i < 8; i++) QuestEvents.slew(player, zombie);
            h.assertTrue(data.step(player.getUUID(), tribe) == 4, "Eight slain near the hearth moves to the rite");
            QuestEvents.rite(h.getLevel(), h.absolutePos(rel), player);
            h.assertTrue(data.step(player.getUUID(), tribe) == 5, "A rite in the camp moves to the dish");
            player.getInventory().add(new ItemStack(tk.darrow.tribalpower.cuisine.CuisineRegistry.dish(tribe)));
            h.assertTrue(Questline.handIn(player, tribe) && data.step(player.getUUID(), tribe) == 6, "The dish moves to the trial");
            // The record survives a save and load.
            CompoundTag saved = data.save(new CompoundTag(), player.server.registryAccess());
            QuestSavedData loaded = QuestSavedData.load(saved, player.server.registryAccess());
            h.assertTrue(loaded.step(player.getUUID(), tribe) == 6, "Step survives save");
            h.assertTrue(loaded.completed(player.getUUID(), tribe) == data.completed(player.getUUID(), tribe), "Completed count survives save");
            QuestEvents.trial(player, tribe);
            h.assertTrue(data.step(player.getUUID(), tribe) == Questline.STEPS && data.hasRelic(player.getUUID(), tribe), "The trial finishes the story and grants the relic");
            h.assertTrue(QuestRegistry.carriesRelic(player, tribe), "The relic lands in the inventory");
            h.assertTrue(player.getAdvancements().getOrStartProgress(player.server.getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath("tribalpower", "tribes/story_soil"))).isDone(), "The story advancement is awarded");
            QuestStatePayload state = QuestStatePayload.of(player);
            h.assertTrue(state.of(tribe).relic() && state.of(tribe).step() == Questline.STEPS, "The Codex state reports the finished story");
        } finally {
            player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }
}
