package tk.darrow.tribalpower.tribe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import tk.darrow.tribalpower.TribalPower;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datapack stall tables under {@code data/<ns>/tribalpower/dock_shop/*.json}.
 * Packs own the currency; this mod never names another mod's items in Java.
 */
public final class DockShop {
    public static final String DIRECTORY = "tribalpower/dock_shop";
    public static final String DEFAULT_STALL = "dock";

    public record Listing(String stall, ItemStack cost, ItemStack result, int maxUses) {}

    private static volatile List<Listing> LISTINGS = List.of();

    private DockShop() {}

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Reloader());
    }

    /** Test hook: replace the live table. */
    public static void replace(List<Listing> listings) {
        LISTINGS = List.copyOf(listings);
    }

    public static MerchantOffers offers(String stall) {
        return offers(stall, new int[0]);
    }

    /** Offers for {@code stall} with {@code uses[i]} already spent on the i-th listing. */
    public static MerchantOffers offers(String stall, int[] uses) {
        String key = stall == null || stall.isBlank() ? DEFAULT_STALL : stall;
        MerchantOffers out = new MerchantOffers();
        for (Listing listing : LISTINGS) {
            if (!listing.stall.equals(key)) continue;
            int i = out.size();
            int used = i < uses.length ? Math.min(uses[i], listing.maxUses) : 0;
            out.add(new MerchantOffer(
                    new ItemCost(listing.cost.getItem(), listing.cost.getCount()), java.util.Optional.empty(),
                    listing.result.copy(),
                    used, listing.maxUses, 0, 0.0F));
        }
        return out;
    }

    private static final class Reloader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().setLenient().create();

        Reloader() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
            List<Listing> next = new ArrayList<>();
            for (var entry : object.entrySet()) {
                try {
                    JsonObject json = entry.getValue().getAsJsonObject();
                    String stall = json.has("stall") ? json.get("stall").getAsString() : DEFAULT_STALL;
                    ItemStack cost = stack(json.getAsJsonObject("cost"));
                    ItemStack result = stack(json.getAsJsonObject("result"));
                    int maxUses = json.has("max_uses") ? json.get("max_uses").getAsInt() : 16;
                    if (!cost.isEmpty() && !result.isEmpty()) next.add(new Listing(stall, cost, result, Math.max(1, maxUses)));
                } catch (RuntimeException e) {
                    TribalPower.LOGGER.warn("Bad dock shop {}: {}", entry.getKey(), e.toString());
                }
            }
            LISTINGS = List.copyOf(next);
            TribalPower.LOGGER.info("Loaded {} dock shop listings", LISTINGS.size());
        }
    }

    private static ItemStack stack(JsonObject json) {
        String id = json.get("id").getAsString();
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, count));
    }
}
