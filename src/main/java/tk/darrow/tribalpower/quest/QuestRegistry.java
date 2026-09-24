package tk.darrow.tribalpower.quest;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/** Tribes that talk: dialogue, requests, questlines and the relics at the end of them. */
public final class QuestRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final Map<TribeDefinition, DeferredItem<RelicItem>> RELICS = new EnumMap<>(TribeDefinition.class);

    static {
        for (TribeDefinition tribe : TribeDefinition.values())
            RELICS.put(tribe, ITEMS.register("relic_" + tribe.id(), () -> new RelicItem(tribe, new Item.Properties().rarity(Rarity.EPIC))));
    }

    private QuestRegistry() {}

    /** Whether the player carries a tribe's relic. */
    public static boolean carriesRelic(net.minecraft.world.entity.player.Player player, TribeDefinition tribe) {
        Item relic = RELICS.get(tribe).get();
        for (ItemStack stack : player.getInventory().items) if (stack.is(relic)) return true;
        return player.getOffhandItem().is(relic);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(DialogueSession::register);
        NeoForge.EVENT_BUS.addListener(Dialogue::register);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) QuestEvents.sync(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) QuestEvents.sync(player);
        });
    }

    public static void displayItems(CreativeModeTab.Output out) {
        RELICS.values().forEach(relic -> out.accept(relic.get()));
    }
}
