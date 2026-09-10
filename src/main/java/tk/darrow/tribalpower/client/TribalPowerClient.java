package tk.darrow.tribalpower.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import tk.darrow.tribalpower.TribalPower;

@Mod(value = TribalPower.MOD_ID, dist = Dist.CLIENT)
public final class TribalPowerClient {
    public TribalPowerClient(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) -> {
            if (event.getLevel().isClientSide && event.getItemStack().is(tk.darrow.tribalpower.item.ModItems.SPIRIT_CODEX.get())) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new SpiritCodexScreen());
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event)->event.register(net.minecraft.resources.ResourceLocation.parse("tribalpower:the_march"),new MarchSkyEffects()));
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,SkyConfig.SPEC);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(AuroraSky::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(VisualVerification::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(VisualVerification::screen);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::gui);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ShowcaseVerification::screen);
        modBus.addListener(TribalColors::items);
        modBus.addListener(TribalColors::blocks);
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            for(var p:tk.darrow.tribalpower.entity.CreatureProfile.values())event.registerLayerDefinition(LatticeCreatureRenderer.layer(p),()->GeneratedCreatureLayers.create(p.id));
            event.registerLayerDefinition(MarchCreatureModel.WALKER, MarchCreatureModel::walker);
            event.registerLayerDefinition(MarchCreatureModel.WISP, MarchCreatureModel::wisp);
            event.registerLayerDefinition(TribalKinModel.LAYER, TribalKinModel::create);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(tk.darrow.tribalpower.echo.StationMenu.TYPE.get(), StationScreen::new));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PulseHud::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.ley.LeyLensHud::render);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(tk.darrow.tribalpower.familiar.FamiliarRegistry.SADDLEBAG.get(), MossbackScreen::new));
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(tk.darrow.tribalpower.boss.client.TheUnsungModel.LAYER, tk.darrow.tribalpower.boss.client.TheUnsungModel::create));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) -> {
            if (event.getLevel().isClientSide && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && !event.getEntity().isSpectator() && !event.getEntity().isShiftKeyDown() && event.getLevel().getBlockEntity(event.getPos()) instanceof tk.darrow.tribalpower.world.structure.LoreTabletBlockEntity tablet)
                net.minecraft.client.Minecraft.getInstance().setScreen(new LoreTabletScreen(tablet.tablet()));
        });
        modBus.addListener(TribeClient::onClientSetup);
        modBus.addListener(TribeClient::blockColours);
        modBus.addListener(TribeClient::itemColours);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> CodexUnlocks.reset());
    }
}
