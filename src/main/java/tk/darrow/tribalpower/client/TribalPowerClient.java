package tk.darrow.tribalpower.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import tk.darrow.tribalpower.TribalPower;

@Mod(value = TribalPower.MOD_ID, dist = Dist.CLIENT)
public final class TribalPowerClient {
    public TribalPowerClient(IEventBus modBus) {
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            event.registerLayerDefinition(MarchCreatureModel.WALKER, MarchCreatureModel::walker);
            event.registerLayerDefinition(MarchCreatureModel.WISP, MarchCreatureModel::wisp);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(tk.darrow.tribalpower.echo.StationMenu.TYPE.get(), StationScreen::new));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PulseHud::render);
    }
}
