package tk.darrow.tribalpower.charm;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import tk.darrow.tribalpower.TribalPower;

import java.util.ArrayList;
import java.util.List;

public final class CharmSlots {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TribalPower.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CharmInventory>> SLOTS =
            ATTACHMENTS.register("charm_slots", () -> AttachmentType.builder(CharmInventory::new)
                    .serialize(CharmInventory.SERIALIZER)
                    .copyOnDeath()
                    .build());

    private CharmSlots() {}

    public static void register(IEventBus modBus) { ATTACHMENTS.register(modBus); }

    public static CharmInventory of(Player player) {
        return player.getData(SLOTS);
    }

    public static List<ItemStack> equipped(Player player) {
        CharmInventory inv = of(player);
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < CharmInventory.SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }
}
