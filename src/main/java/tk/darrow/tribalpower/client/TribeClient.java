package tk.darrow.tribalpower.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRegistry;

/** Item model predicate {@code tribalpower:tribe} (ordinal / 10) so one item id shows nine tribe models. */
public final class TribeClient {
    private TribeClient() {}

    /** Hearth embers (block tint 0 / item layer 1) take the tribe colour. */
    public static void blockColours(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 && level != null && pos != null
                && level.getBlockEntity(pos) instanceof tk.darrow.tribalpower.tribe.TribeHearthBlockEntity hearth
                ? 0xFF000000 | hearth.tribe().colour() : 0xFFFFFFFF, TribeRegistry.TRIBE_HEARTH.get());
    }

    public static void itemColours(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 1 ? 0xFF000000 | TribeDefinition.ofOrDefault(stack).colour() : 0xFFFFFFFF, TribeRegistry.TRIBE_HEARTH_ITEM.get());
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation key = ResourceLocation.parse("tribalpower:tribe");
            for (var item : new net.minecraft.world.item.Item[]{TribeRegistry.TRIBE_MARK.get(), TribeRegistry.TRIBE_HEARTH_ITEM.get(),
                    TribeRegistry.TRIBE_BANNER_ITEM.get(), TribeRegistry.KINSHIP_TOTEM_ITEM.get()})
                ItemProperties.register(item, key, (stack, level, entity, seed) -> TribeDefinition.ofOrDefault(stack).ordinal() / 10F);
        });
    }
}
