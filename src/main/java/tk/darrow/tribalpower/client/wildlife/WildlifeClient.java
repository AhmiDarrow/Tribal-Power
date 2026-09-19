package tk.darrow.tribalpower.client.wildlife;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import tk.darrow.tribalpower.wildlife.Wildlife;

/** Client wiring for the March's wildlife: models, renderers and the insect particles. */
public final class WildlifeClient {
    private static final String[] KINDS = {"glimmerfin", "drift_bell", "veil_ray", "silt_eel", "loom_swift"};

    private WildlifeClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            for (String kind : KINDS) event.registerLayerDefinition(WildlifeModel.layer(kind), () -> GeneratedWildlifeLayers.create(kind));
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(Wildlife.GLIMMERFIN.get(), ctx -> new WildlifeRenderer<>(ctx, "glimmerfin", 0.2F, false, true));
            event.registerEntityRenderer(Wildlife.DRIFT_BELL.get(), ctx -> new WildlifeRenderer<>(ctx, "drift_bell", 0.3F, true, true));
            event.registerEntityRenderer(Wildlife.VEIL_RAY.get(), ctx -> new WildlifeRenderer<>(ctx, "veil_ray", 0.6F, false, true));
            event.registerEntityRenderer(Wildlife.SILT_EEL.get(), ctx -> new WildlifeRenderer<>(ctx, "silt_eel", 0.3F, false, true));
            event.registerEntityRenderer(Wildlife.LOOM_SWIFT.get(), ctx -> new WildlifeRenderer<>(ctx, "loom_swift", 0.15F, false, false));
        });
        modBus.addListener(MarchInsects::providers);
        NeoForge.EVENT_BUS.addListener(MarchInsects::tick);
    }
}
