package tk.darrow.tribalpower.entity;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityAttributes {
    private ModEntityAttributes() {}

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SPIRIT_WISP.get(), SpiritWispEntity.createAttributes().build());
        event.put(ModEntities.MARCH_WALKER.get(), MarchWalkerEntity.createAttributes().build());
    }
}
