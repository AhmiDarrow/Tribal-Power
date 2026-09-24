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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MarchWeatherClient::tick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MarchMusic::tick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MarchWeatherClient::fog);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MarchWeatherClient::fogColor);
        installDevHook("tk.darrow.tribalpower.client.VisualVerification");
        installDevHook("tk.darrow.tribalpower.client.ShowcaseVerification");
        modBus.addListener(PanoramicSky::registerShaders);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) -> event.registerReloadListener(new tk.darrow.tribalpower.client.codex.CodexBook.Loader()));
        modBus.addListener(TribalColors::items);
        modBus.addListener(VoiceGlow::items);
        modBus.addListener(VoiceGlow::layers);
        modBus.addListener(TribalColors::blocks);
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener(ClientSetup::registerRenderers);
        tk.darrow.tribalpower.client.wildlife.WildlifeClient.register(modBus);
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            for(var p:tk.darrow.tribalpower.entity.CreatureProfile.values())event.registerLayerDefinition(LatticeCreatureRenderer.layer(p),()->GeneratedCreatureLayers.create(p.id));
            event.registerLayerDefinition(MarchCreatureModel.WALKER, MarchCreatureModel::walker);
            event.registerLayerDefinition(MarchCreatureModel.WISP, MarchCreatureModel::wisp);
            event.registerLayerDefinition(TribalKinModel.LAYER, TribalKinModel::create);
            for (var guardian : tk.darrow.tribalpower.guardian.Guardian.values())
                event.registerLayerDefinition(tk.darrow.tribalpower.guardian.client.GuardianRenderer.layer(guardian), () -> GeneratedGuardianLayers.create(guardian.id));
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) -> {
                event.register(tk.darrow.tribalpower.echo.StationMenu.TYPE.get(), StationScreen::new);
                event.register(tk.darrow.tribalpower.healing.HealingRegistry.KETTLE_MENU.get(), KettleScreen::new);
                event.register(tk.darrow.tribalpower.cuisine.CuisineRegistry.HEARTH_POT_MENU.get(), HearthPotScreen::new);
                event.register(tk.darrow.tribalpower.echo.RelayMenu.TYPE.get(), RelayScreen::new);
                event.register(tk.darrow.tribalpower.echo.CacheMenu.TYPE.get(), CacheScreen::new);
                event.register(tk.darrow.tribalpower.charm.CharmMenu.TYPE.get(), CharmScreen::new);
                event.register(tk.darrow.tribalpower.device.DeviceRegistry.SEAL_LOOM_MENU.get(), SealLoomScreen::new);
                event.register(tk.darrow.tribalpower.bench.BenchRegistry.MENU.get(), BenchScreen::new);
                event.register(tk.darrow.tribalpower.echo.ModMenus.SONG_BENCH.get(), SongBenchScreen::new);
                event.register(tk.darrow.tribalpower.echo.ModMenus.REAGENT_POUCH.get(), PouchScreen::new);
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PulseHud::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FamiliarInspectHud::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.ley.LeyLensHud::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LeyRopeRenderer::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.building.ChalkGhostRenderer::render);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(tk.darrow.tribalpower.familiar.FamiliarRegistry.SADDLEBAG.get(), MossbackScreen::new));
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(tk.darrow.tribalpower.boss.client.TheUnsungModel.LAYER, tk.darrow.tribalpower.boss.client.TheUnsungModel::create));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) -> {
            if (event.getLevel().isClientSide && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && !event.getEntity().isSpectator() && !event.getEntity().isShiftKeyDown() && event.getLevel().getBlockEntity(event.getPos()) instanceof tk.darrow.tribalpower.world.structure.LoreTabletBlockEntity tablet)
                net.minecraft.client.Minecraft.getInstance().setScreen(new LoreTabletScreen(tablet.tablet()));
            if (event.getLevel().isClientSide && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && !event.getEntity().isSpectator() && !event.getEntity().isShiftKeyDown()) {
                var carved = event.getLevel().getBlockState(event.getPos());
                if (carved.hasProperty(tk.darrow.tribalpower.lore.CarvedStoneBlock.FRAGMENT))
                    net.minecraft.client.Minecraft.getInstance().setScreen(new FragmentScreen(carved.getValue(tk.darrow.tribalpower.lore.CarvedStoneBlock.FRAGMENT)));
            }
        });
        modBus.addListener(TribeClient::onClientSetup);
        modBus.addListener(TribeClient::blockColours);
        modBus.addListener(TribeClient::itemColours);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ItemHints::tooltip);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SortButtons::onScreenInit);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> {
            CodexUnlocks.reset();
            tk.darrow.tribalpower.ley.LeyRopePayload.latest = new tk.darrow.tribalpower.ley.LeyRopePayload(java.util.List.of());
            // Readings from the last world must not stand in for this one's.
            tk.darrow.tribalpower.ley.LensPulsePayload.latest = tk.darrow.tribalpower.ley.LensPulsePayload.empty();
            tk.darrow.tribalpower.ley.LeySightPayload.latest = new tk.darrow.tribalpower.ley.LeySightPayload(0, 0, 0);
            tk.darrow.tribalpower.ley.LeySightPayload.seen = false;
            tk.darrow.tribalpower.quest.QuestStatePayload.latest = tk.darrow.tribalpower.quest.QuestStatePayload.EMPTY;
            tk.darrow.tribalpower.event.MarchStatePayload.latest = tk.darrow.tribalpower.event.MarchStatePayload.EMPTY;
            MarchWeatherClient.reset();
            MarchMusic.reset();
        });
    }

    /** Screenshot drivers stay on the compile classpath for Gradle runs; the published jar omits them. */
    private static void installDevHook(String className) {
        try {
            Class.forName(className).getMethod("install").invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
