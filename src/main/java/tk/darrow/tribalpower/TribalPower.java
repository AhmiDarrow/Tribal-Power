package tk.darrow.tribalpower;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.entity.ModEntities;
import tk.darrow.tribalpower.entity.ModEntityAttributes;
import tk.darrow.tribalpower.item.ModCreativeTabs;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.world.ModDimensions;

@Mod(TribalPower.MOD_ID)
public final class TribalPower {
    public static final String MOD_ID = "tribalpower";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TribalPower(IEventBus modBus) {
        tk.darrow.tribalpower.camp.CampRegistry.BLOCKS.register(modBus);
        tk.darrow.tribalpower.camp.CampRegistry.ITEMS.register(modBus);
        tk.darrow.tribalpower.camp.CampRegistry.ENTITIES.register(modBus);
        modBus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event)->event.register(tk.darrow.tribalpower.camp.CampHooks.TICKETS));
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.camp.CampHooks::spawn);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.camp.CampHooks::finalizeSpawn);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        tk.darrow.tribalpower.entity.CreatureEntities.ENTITIES.register(modBus);
        tk.darrow.tribalpower.item.CreatureItems.ITEMS.register(modBus);
        modBus.addListener(tk.darrow.tribalpower.entity.CreatureEntities::attributes);
        modBus.addListener(tk.darrow.tribalpower.entity.CreatureEntities::placements);
        ModCreativeTabs.TABS.register(modBus);
        tk.darrow.tribalpower.echo.StationMenu.MENUS.register(modBus);
        tk.darrow.tribalpower.echo.LatticeRecipe.TYPES.register(modBus);
        tk.darrow.tribalpower.echo.LatticeRecipe.SERIALIZERS.register(modBus);
        tk.darrow.tribalpower.item.SpiritweaveArmor.MATERIALS.register(modBus);
        tk.darrow.tribalpower.familiar.FamiliarRegistry.ITEMS.register(modBus);
        tk.darrow.tribalpower.familiar.FamiliarRegistry.BLOCKS.register(modBus);
        tk.darrow.tribalpower.familiar.FamiliarRegistry.MENUS.register(modBus);
        tk.darrow.tribalpower.camp.identity.CampIdentityRegistry.ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.camp.identity.CampCommands::register);
        tk.darrow.tribalpower.camp.identity.CampStanding.register();
        tk.darrow.tribalpower.world.structure.MarchRegistry.register(modBus);
        tk.darrow.tribalpower.tribe.TribeRegistry.register(modBus);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.TribeHooks::onDeath);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.TribeHooks::onBreak);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.TribeHooks::onCommands);

        modBus.addListener(this::onCommonSetup);
        // Rites / QoL (rite/world, ley, logic, api/Diagnostics)
        tk.darrow.tribalpower.rite.world.WorldRiteRegistry.register(modBus);
        tk.darrow.tribalpower.ley.LeyRegistry.register(modBus);
        tk.darrow.tribalpower.logic.LogicRegistry.register(modBus);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.api.Diagnostics::onRightClickBlock);
        modBus.addListener(ModEntityAttributes::onAttributes);
        modBus.addListener(ModEntityAttributes::onSpawnPlacements);
        NeoForge.EVENT_BUS.register(ModDimensions.class);
        modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    tk.darrow.tribalpower.camp.CampRegistry.TYPE.get(),(be,side)->be.hasInventory()?new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                            new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be,side==null?net.minecraft.core.Direction.UP:side)):null);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.SPIRIT_CISTERN.get(), (be, side) -> be.tank);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.PULSE_ADAPTER.get(), (be, side) -> be.handler);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.ECHO_STATION.get(), (be, side) ->
                            new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                            side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                    : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.ANCESTRAL_CACHE.get(), (be, side) -> new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be, new net.neoforged.neoforge.items.wrapper.InvWrapper(be)));
        });
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.info("Tribal Power — shamanic technomancy online"));
    }
}
