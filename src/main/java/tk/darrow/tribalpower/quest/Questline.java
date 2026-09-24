package tk.darrow.tribalpower.quest;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;

/**
 * Each tribe's story with the player: seven steps, told by its Elder, that end at the tribe's guardian trial.
 * The steps lean on what already exists -- standing, requests, rites, dishes -- and the last waits on the
 * guardian of the tribe's country (the Loom-stitchers' on The Unsung).
 */
public final class Questline {
    public enum Kind { TALK, RANK, FETCH, SLAY, RITE, DELIVER, TRIAL }

    /** One step: what it asks, how much, and the item where one is wanted. */
    public record Step(Kind kind, int count, ItemStack item, TribeRank rank) {
        public Component describe(TribeDefinition tribe) {
            return switch (kind) {
                case TALK -> Component.translatable("questline.tribalpower.step.talk", tribe.displayNameComponent());
                case RANK -> Component.translatable("questline.tribalpower.step.rank", Component.translatable(rank.translationKey()), tribe.displayNameComponent());
                case FETCH -> Component.translatable("questline.tribalpower.step.fetch", count, item.getHoverName());
                case SLAY -> Component.translatable("questline.tribalpower.step.slay", count, tribe.displayNameComponent());
                case RITE -> Component.translatable("questline.tribalpower.step.rite", tribe.displayNameComponent());
                case DELIVER -> Component.translatable("questline.tribalpower.step.deliver", count, item.getHoverName());
                case TRIAL -> Component.translatable("questline.tribalpower.step.trial." + tribe.id());
            };
        }
    }

    public static final int STEPS = 7;

    private Questline() {}

    private static Step talk() { return new Step(Kind.TALK, 1, ItemStack.EMPTY, TribeRank.STRANGER); }
    private static Step rank(TribeRank rank) { return new Step(Kind.RANK, 1, ItemStack.EMPTY, rank); }
    private static Step fetch(net.minecraft.world.level.ItemLike item, int count) { return new Step(Kind.FETCH, count, new ItemStack(item), TribeRank.STRANGER); }
    private static Step slay(int count) { return new Step(Kind.SLAY, count, ItemStack.EMPTY, TribeRank.STRANGER); }
    private static Step rite() { return new Step(Kind.RITE, 1, ItemStack.EMPTY, TribeRank.STRANGER); }
    private static Step deliver(TribeDefinition tribe) { return new Step(Kind.DELIVER, 1, new ItemStack(CuisineRegistry.dish(tribe)), TribeRank.STRANGER); }
    private static Step trial() { return new Step(Kind.TRIAL, 1, ItemStack.EMPTY, TribeRank.STRANGER); }

    /** The seven steps of a tribe's story. Each tribe's fetch is the thing it prizes. */
    public static List<Step> steps(TribeDefinition tribe) {
        var fetched = switch (tribe) {
            case SOIL -> fetch(net.minecraft.world.item.Items.MOSS_BLOCK, 16);
            case STONE -> fetch(tk.darrow.tribalpower.item.ModItems.IRON_GRIT.get(), 8);
            case SPROUT -> fetch(tk.darrow.tribalpower.block.ModBlocks.SPIRIT_REED.get(), 12);
            case CLAW -> fetch(tk.darrow.tribalpower.item.ModItems.MARCH_LEATHER.get(), 6);
            case SPARK -> fetch(tk.darrow.tribalpower.item.ModItems.ATTUNED_ECHO.get(), 4);
            case CLOCK -> fetch(net.minecraft.world.item.Items.CLOCK, 1);
            case SWARM -> fetch(net.minecraft.world.item.Items.HONEY_BLOCK, 2);
            case SIGIL -> fetch(tk.darrow.tribalpower.item.ModItems.SPIRIT_SEAL.get(), 1);
            case SPINDLE -> fetch(tk.darrow.tribalpower.item.ModItems.MARCH_CRYSTAL.get(), 4);
        };
        return List.of(talk(), rank(TribeRank.FRIEND), fetched, slay(8), rite(), deliver(tribe), trial());
    }

    public static Step step(TribeDefinition tribe, int index) {
        List<Step> steps = steps(tribe);
        return index < 0 || index >= steps.size() ? null : steps.get(index);
    }

    public static boolean done(ServerPlayer player, TribeDefinition tribe) {
        return QuestSavedData.get(player.server).step(player.getUUID(), tribe) >= STEPS;
    }

    public static String stepKey(TribeDefinition tribe, int index) {
        return "dialogue.tribalpower." + tribe.id() + ".quest." + index;
    }

    /** Moves the story on one step, telling the player and handing the relic over at the end. */
    static void advance(ServerPlayer player, TribeDefinition tribe) {
        QuestSavedData data = QuestSavedData.get(player.server);
        int step = data.step(player.getUUID(), tribe);
        if (step >= STEPS) return;
        data.setStep(player.getUUID(), tribe, step + 1, 0);
        player.sendSystemMessage(Component.translatable("message.tribalpower.questline.step", tribe.displayNameComponent(), step + 1, STEPS)
                .withStyle(tk.darrow.tribalpower.tribe.TribeStanding.colour(tribe)));
        if (step + 1 >= STEPS) {
            if (!data.hasRelic(player.getUUID(), tribe)) {
                data.grantRelic(player.getUUID(), tribe);
                tk.darrow.tribalpower.item.SpiritgearHelper.give(player, new ItemStack(QuestRegistry.RELICS.get(tribe).get()));
            }
            tk.darrow.tribalpower.camp.CampHooks.award(player.serverLevel(), player.getUUID(), "tribes/story_" + tribe.id());
            player.sendSystemMessage(Component.translatable("message.tribalpower.questline.done", tribe.displayNameComponent())
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            tk.darrow.tribalpower.tribe.CodexUnlocksPayload.sync(player);
        }
        QuestEvents.sync(player);
    }

    /** Progress on a counted step, advancing when it is met. */
    static void progress(ServerPlayer player, TribeDefinition tribe, Kind kind, int by) {
        QuestSavedData data = QuestSavedData.get(player.server);
        int index = data.step(player.getUUID(), tribe);
        Step step = step(tribe, index);
        if (step == null || step.kind() != kind) return;
        int progress = data.stepProgress(player.getUUID(), tribe) + by;
        if (progress >= step.count()) advance(player, tribe);
        else data.setStep(player.getUUID(), tribe, index, progress);
        QuestEvents.sync(player);
    }

    /** Whether the current step can be handed in at the Elder: a fetch or delivery the player carries, or a rank reached. */
    public static boolean ready(ServerPlayer player, TribeDefinition tribe) {
        QuestSavedData data = QuestSavedData.get(player.server);
        Step step = step(tribe, data.step(player.getUUID(), tribe));
        if (step == null) return false;
        return switch (step.kind()) {
            case TALK -> true;
            case RANK -> tk.darrow.tribalpower.camp.identity.CampStanding.effectiveStanding(player, tribe) >= step.rank().threshold();
            case FETCH, DELIVER -> {
                int have = 0;
                for (ItemStack stack : player.getInventory().items) if (ItemStack.isSameItem(stack, step.item())) have += stack.getCount();
                yield have >= step.count();
            }
            default -> false;
        };
    }

    /** Hands the current step in at the Elder, taking what it asked for. */
    public static boolean handIn(ServerPlayer player, TribeDefinition tribe) {
        if (!ready(player, tribe)) return false;
        QuestSavedData data = QuestSavedData.get(player.server);
        Step step = step(tribe, data.step(player.getUUID(), tribe));
        if (step.kind() == Kind.FETCH || step.kind() == Kind.DELIVER) {
            int left = step.count();
            for (int i = 0; i < player.getInventory().items.size() && left > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (!ItemStack.isSameItem(stack, step.item())) continue;
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
        advance(player, tribe);
        return true;
    }

    public static String kindId(Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }
}
