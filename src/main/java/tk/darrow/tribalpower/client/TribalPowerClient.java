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
    }
}
