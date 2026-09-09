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
        tk.darrow.tribalpower.entity.CreatureEntities.ANIMALS.forEach((p,t)->event.registerEntityRenderer(t.get(),context->new LatticeCreatureRenderer<>(context,p)));
        tk.darrow.tribalpower.entity.CreatureEntities.MONSTERS.forEach((p,t)->event.registerEntityRenderer(t.get(),context->new LatticeCreatureRenderer<>(context,p)));
        event.registerEntityRenderer(ModEntities.SPIRIT_WISP.get(), SpiritWispRenderer::new);
        event.registerEntityRenderer(ModEntities.MARCH_WALKER.get(), MarchWalkerRenderer::new);
        event.registerEntityRenderer(tk.darrow.tribalpower.world.structure.MarchRegistry.THE_UNSUNG.get(), tk.darrow.tribalpower.boss.client.TheUnsungRenderer::new);
        event.registerEntityRenderer(tk.darrow.tribalpower.tribe.TribeRegistry.TRIBAL_KIN.get(), TribalKinRenderer::new);
    }
}
