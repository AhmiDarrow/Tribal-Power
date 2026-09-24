package tk.darrow.tribalpower.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tk.darrow.tribalpower.building.BuildPattern;
import tk.darrow.tribalpower.building.BuildersChalkItem;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.cuisine.CuisineRegistry;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeHooks;
import tk.darrow.tribalpower.tribe.TribeStanding;

/**
 * A tribe's requests: small, repeatable work an Elder asks of a friend. Each tribe has a pool in its own way,
 * and each day three of them are open. Fetch, slay, build, perform a rite, or deliver a dish; the reward is
 * standing, and every third finished request is paid in Tribe Marks.
 */
public final class Requests {
    public enum Kind { FETCH, SLAY, BUILD, RITE, DELIVER }

    /** One request: what is wanted, how much, and what it pays. {@code item} is for FETCH and DELIVER, {@code shape} for BUILD. */
    public record Template(String id, Kind kind, ItemStack item, int count, int standing, BuildPattern shape, int size) {
        public String key() { return "request.tribalpower." + id; }
        public Component name() { return Component.translatable(key()); }
        public Component describe() {
            return switch (kind) {
                case FETCH, DELIVER -> Component.translatable("request.tribalpower.describe." + kind.name().toLowerCase(Locale.ROOT), count, item.getHoverName());
                case SLAY -> Component.translatable("request.tribalpower.describe.slay", count);
                case BUILD -> Component.translatable("request.tribalpower.describe.build", Component.translatable(shape.key()), size);
                case RITE -> Component.translatable("request.tribalpower.describe.rite");
            };
        }
    }

    public static final int OPEN_PER_DAY = 3;
    /** Blocks around one of the tribe's hearths that a slaying, a build or a rite must happen within. */
    public static final int CAMP_RADIUS = 48;

    private Requests() {}

    private static Template fetch(TribeDefinition tribe, String id, net.minecraft.world.level.ItemLike item, int count, int standing) {
        return new Template(tribe.id() + "." + id, Kind.FETCH, new ItemStack(item), count, standing, null, 0);
    }

    /** Every request a tribe may make, in its own voice. */
    public static List<Template> pool(TribeDefinition tribe) {
        List<Template> out = new ArrayList<>();
        String t = tribe.id();
        out.add(fetch(tribe, "crop", CuisineRegistry.CROP_ITEMS.get(tribe.dishCrop()).get(), 12, 10));
        out.add(new Template(t + ".dish", Kind.DELIVER, new ItemStack(CuisineRegistry.dish(tribe)), 2, 14, null, 0));
        out.add(new Template(t + ".hunt", Kind.SLAY, ItemStack.EMPTY, 6, 12, null, 0));
        out.add(new Template(t + ".rite", Kind.RITE, ItemStack.EMPTY, 1, 16, null, 0));
        out.add(new Template(t + ".wall", Kind.BUILD, ItemStack.EMPTY, 1, 14, BuildPattern.WALLS, 3));
        switch (tribe) {
            case SOIL -> out.add(fetch(tribe, "moss", Items.MOSS_BLOCK, 8, 8));
            case STONE -> out.add(fetch(tribe, "grit", ModItems.IRON_GRIT.get(), 6, 12));
            case SPROUT -> out.add(fetch(tribe, "reed", tk.darrow.tribalpower.block.ModBlocks.SPIRIT_REED.get(), 8, 8));
            case CLAW -> out.add(fetch(tribe, "leather", ModItems.MARCH_LEATHER.get(), 4, 10));
            case SPARK -> out.add(fetch(tribe, "charcoal", Items.CHARCOAL, 16, 8));
            case CLOCK -> out.add(fetch(tribe, "redstone", Items.REDSTONE, 16, 8));
            case SWARM -> out.add(fetch(tribe, "honey", Items.HONEYCOMB, 6, 10));
            case SIGIL -> out.add(fetch(tribe, "chalk", ModItems.RITUAL_CHALK.get(), 2, 10));
            case SPINDLE -> out.add(fetch(tribe, "thistle", tk.darrow.tribalpower.block.ModBlocks.LEY_THISTLE.get(), 6, 10));
        }
        return List.copyOf(out);
    }

    public static Template template(TribeDefinition tribe, String id) {
        for (Template template : pool(tribe)) if (template.id().equals(id)) return template;
        return null;
    }

    /** The three requests a tribe has open today, drawn from its pool by the day and the tribe. */
    public static List<Template> openToday(TribeDefinition tribe, long day) {
        List<Template> pool = pool(tribe);
        List<Template> out = new ArrayList<>();
        int start = (int) Math.floorMod(day * 2 + tribe.ordinal() * 7L, pool.size());
        for (int i = 0; i < Math.min(OPEN_PER_DAY, pool.size()); i++) out.add(pool.get((start + i * 2) % pool.size()));
        return out;
    }

    public static long day(ServerLevel level) {
        return level.getServer().overworld().getDayTime() / 24000L;
    }

    /** Whether the player stands within a tribe's camp: near one of its hearths. */
    public static boolean atCamp(ServerLevel level, BlockPos pos, TribeDefinition tribe) {
        return TribeHooks.hearthsNear(level, pos, CAMP_RADIUS).contains(tribe);
    }

    /** Whether a FETCH or DELIVER can be turned in: the player carries enough. */
    public static boolean carries(ServerPlayer player, Template template) {
        int have = 0;
        for (ItemStack stack : player.getInventory().items) if (ItemStack.isSameItem(stack, template.item())) have += stack.getCount();
        return have >= template.count();
    }

    private static void take(ServerPlayer player, Template template) {
        int left = template.count();
        for (int i = 0; i < player.getInventory().items.size() && left > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!ItemStack.isSameItem(stack, template.item())) continue;
            int taken = Math.min(left, stack.getCount());
            stack.shrink(taken);
            left -= taken;
        }
    }

    /** Whether a BUILD is standing: the player's chalk marks the shape, near the camp, and every block of it is solid. */
    public static boolean built(ServerPlayer player, TribeDefinition tribe, Template template) {
        ItemStack chalk = player.getMainHandItem().is(ModItems.BUILDERS_CHALK.get()) ? player.getMainHandItem()
                : player.getOffhandItem().is(ModItems.BUILDERS_CHALK.get()) ? player.getOffhandItem() : ItemStack.EMPTY;
        if (chalk.isEmpty() || BuildersChalkItem.shape(chalk) != template.shape() || BuildersChalkItem.size(chalk) < template.size()) return false;
        BlockPos mark = BuildersChalkItem.mark(chalk);
        if (mark == null || !atCamp(player.serverLevel(), mark, tribe)) return false;
        for (BlockPos offset : template.shape().offsets(BuildersChalkItem.size(chalk)))
            if (!player.serverLevel().getBlockState(mark.offset(offset)).isSolid()) return false;
        return true;
    }

    // ---- the flow -------------------------------------------------------------------------------------------------

    /** The Elder offers today's first open request the player is not already on. Returns the offered template or null. */
    public static Template offer(ServerPlayer player, TribeDefinition tribe) {
        QuestSavedData data = QuestSavedData.get(player.server);
        if (data.request(player.getUUID(), tribe) != null) return template(tribe, data.request(player.getUUID(), tribe).template());
        long day = day(player.serverLevel());
        List<Template> open = openToday(tribe, day);
        if (open.isEmpty()) return null;
        Template chosen = open.get((int) Math.floorMod(player.getUUID().getLeastSignificantBits() + day, open.size()));
        data.setRequest(player.getUUID(), tribe, new QuestSavedData.Request(chosen.id(), day, 0));
        QuestEvents.sync(player);
        return chosen;
    }

    /** Whether the player's open request with a tribe is done and can be turned in. */
    public static boolean ready(ServerPlayer player, TribeDefinition tribe) {
        QuestSavedData data = QuestSavedData.get(player.server);
        QuestSavedData.Request request = data.request(player.getUUID(), tribe);
        if (request == null) return false;
        Template template = template(tribe, request.template());
        if (template == null) return false;
        return switch (template.kind()) {
            case FETCH, DELIVER -> carries(player, template);
            case SLAY, RITE -> request.progress() >= template.count();
            case BUILD -> built(player, tribe, template);
        };
    }

    /** Turns the open request in: takes what was asked, pays standing, and every third one pays a Tribe Mark. */
    public static boolean turnIn(ServerPlayer player, TribeDefinition tribe) {
        if (!ready(player, tribe)) return false;
        QuestSavedData data = QuestSavedData.get(player.server);
        Template template = template(tribe, data.request(player.getUUID(), tribe).template());
        if (template.kind() == Kind.FETCH || template.kind() == Kind.DELIVER) take(player, template);
        data.setRequest(player.getUUID(), tribe, null);
        data.completedOne(player.getUUID(), tribe);
        TribeStanding.add(player, tribe, (int) Math.round(template.standing() * TribalConfig.requestStandingScale()));
        if (data.completed(player.getUUID(), tribe) % TribalConfig.requestsPerMark() == 0)
            tk.darrow.tribalpower.item.SpiritgearHelper.give(player, tribe.stamped(tk.darrow.tribalpower.tribe.TribeRegistry.TRIBE_MARK.get()));
        player.sendSystemMessage(Component.translatable("message.tribalpower.request.done", tribe.displayNameComponent(), template.name())
                .withStyle(TribeStanding.colour(tribe)));
        QuestEvents.sync(player);
        return true;
    }

    /** Progress on the tribe's open request, for the kinds that are counted rather than carried. */
    static void progress(ServerPlayer player, TribeDefinition tribe, Kind kind, int by) {
        QuestSavedData data = QuestSavedData.get(player.server);
        QuestSavedData.Request request = data.request(player.getUUID(), tribe);
        if (request == null) return;
        Template template = template(tribe, request.template());
        if (template == null || template.kind() != kind || request.progress() >= template.count()) return;
        data.setRequest(player.getUUID(), tribe, request.advance(by));
        if (request.progress() + by >= template.count())
            player.displayClientMessage(Component.translatable("message.tribalpower.request.ready", tribe.displayNameComponent(), template.name()), true);
        QuestEvents.sync(player);
    }
}
