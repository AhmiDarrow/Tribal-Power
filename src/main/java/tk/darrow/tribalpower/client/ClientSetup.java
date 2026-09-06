package tk.darrow.tribalpower.client;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import tk.darrow.tribalpower.entity.ModEntities;

public final class ClientSetup {
    private ClientSetup() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        // Client hooks reserved for pulse overlays / lattice line rendering.
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPIRIT_WISP.get(), SpiritWispRenderer::new);
        event.registerEntityRenderer(ModEntities.MARCH_WALKER.get(), MarchWalkerRenderer::new);
    }
}
