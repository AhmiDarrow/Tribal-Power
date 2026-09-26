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

    public TribalPower(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, tk.darrow.tribalpower.config.TribalConfig.SPEC);
        tk.darrow.tribalpower.camp.CampRegistry.BLOCKS.register(modBus);
        tk.darrow.tribalpower.camp.CampRegistry.ITEMS.register(modBus);
        tk.darrow.tribalpower.camp.CampRegistry.ENTITIES.register(modBus);
        modBus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event)->event.register(tk.darrow.tribalpower.camp.CampHooks.TICKETS));
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.camp.CampHooks::spawn);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.world.MarchNights::sleepFinished);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.camp.CampHooks::finalizeSpawn);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.camp.Ownership::guardBreak);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.building.BuildersChalkItem::shrink);
        tk.darrow.tribalpower.world.MarchOres.init();
        tk.darrow.tribalpower.world.MarchBuilding.init();
        tk.darrow.tribalpower.world.MarchWoods.init();
        tk.darrow.tribalpower.world.MarchDecor.init();
        tk.darrow.tribalpower.world.MarchTrees.init();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        tk.darrow.tribalpower.item.SpiritFlaskItem.COMPONENTS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        tk.darrow.tribalpower.entity.CreatureEntities.ENTITIES.register(modBus);
        tk.darrow.tribalpower.item.CreatureItems.ITEMS.register(modBus);
        modBus.addListener(tk.darrow.tribalpower.entity.CreatureEntities::attributes);
        modBus.addListener(tk.darrow.tribalpower.entity.CreatureEntities::placements);
        ModCreativeTabs.TABS.register(modBus);
        tk.darrow.tribalpower.echo.ModMenus.MENUS.register(modBus);
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
        tk.darrow.tribalpower.world.MarchFeatures.register(modBus);
        tk.darrow.tribalpower.wildlife.Wildlife.register(modBus);
        tk.darrow.tribalpower.world.MarchStructures.register(modBus);
        tk.darrow.tribalpower.tribe.TribeRegistry.register(modBus);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.TribeHooks::onDeath);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.TribeHooks::onBreak);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.TribeHooks::onCommands);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.DockShop::register);
        modBus.addListener(tk.darrow.tribalpower.tribe.CodexUnlocksPayload::register);
        modBus.addListener(tk.darrow.tribalpower.lattice.SideIoPayload::register);
        modBus.addListener(tk.darrow.tribalpower.item.GogglesTogglePayload::register);
        modBus.addListener(tk.darrow.tribalpower.item.GearSettingsPayload::register);
        modBus.addListener(tk.darrow.tribalpower.storage.SortPayload::register);
        modBus.addListener(tk.darrow.tribalpower.ley.LensPulsePayload::register);
        modBus.addListener(tk.darrow.tribalpower.ley.LeySightPayload::register);
        modBus.addListener(tk.darrow.tribalpower.ley.LeyRopePayload::register);
        modBus.addListener(tk.darrow.tribalpower.gate.DrumRite::register);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.CodexUnlocksPayload::onLogin);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.CodexUnlocksPayload::onRespawn);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.tribe.CodexUnlocksPayload::onClone);
        tk.darrow.tribalpower.echo.DamagedIngredient.INGREDIENT_TYPES.register(modBus);

        // 3.1: patterns, the metal/gem split, the gates and the six voices.
        tk.darrow.tribalpower.grit.GritItems.register(modBus);
        tk.darrow.tribalpower.gate.GateRegistry.register(modBus);
        tk.darrow.tribalpower.generator.GeneratorRegistry.register(modBus);
        tk.darrow.tribalpower.sound.ModSounds.SOUNDS.register(modBus);

        modBus.addListener(this::onCommonSetup);
        // Rites / QoL (rite/world, ley, logic, api/Diagnostics)
        tk.darrow.tribalpower.rite.world.WorldRiteRegistry.register(modBus);
        tk.darrow.tribalpower.healing.HealingRegistry.register(modBus);
        tk.darrow.tribalpower.kit.KitRegistry.register(modBus);
        tk.darrow.tribalpower.effect.ModEffects.register(modBus);
        tk.darrow.tribalpower.cuisine.CuisineRegistry.register(modBus);
        tk.darrow.tribalpower.quest.QuestRegistry.register(modBus);
        tk.darrow.tribalpower.guardian.GuardianRegistry.register(modBus);
        tk.darrow.tribalpower.event.MarchEvents.register(modBus);
        tk.darrow.tribalpower.lore.LoreRegistry.register(modBus);
        tk.darrow.tribalpower.ley.LeyRegistry.register(modBus);
        tk.darrow.tribalpower.logic.LogicRegistry.register(modBus);
        tk.darrow.tribalpower.device.DeviceRegistry.register(modBus);
        tk.darrow.tribalpower.bench.BenchRegistry.register(modBus);
        tk.darrow.tribalpower.anvil.SpiritAnvil.register(modBus);
        tk.darrow.tribalpower.charm.CharmSlots.register(modBus);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.charm.CharmHooks::playerTick);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.charm.CharmHooks::loggedOut);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.charm.CharmHooks::incomingDamage);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.charm.CharmHooks::drops);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.MachineRank::beforePlace);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.MachineRank::placed);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.MachineRank::dropped);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.MachineRank::playerTick);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::beforeBreak);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::drops);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::incomingDamage);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::dealtDamage);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::keepHealth);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.GearCell::broken);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.GearCell::armorBroken);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGear::rankAttributes);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritgearWeaponItem::attributes);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritgearWeaponItem::incomingDamage);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.song.Anointing::incomingDamage);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.song.Anointing::dealtDamage);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.song.Anointing::tooltip);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::knockback);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::fall);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::trample);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::playerTick);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.SpiritGearHooks::toggleWornGoggles);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.ley.LeyRopes::loggedOut);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.GearCell::stackedOn);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.block.ResonanceTotemBlock::sneakLink);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.song.ReagentPouchHooks::onPickup);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.item.GearCell::tooltip);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.api.Diagnostics::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ModBlocks::tillMarchSoil);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.world.MarchWoods::strip);
        NeoForge.EVENT_BUS.addListener(ModBlocks::flattenMarchSoil);
        modBus.addListener(ModEntityAttributes::onAttributes);
        modBus.addListener(ModEntityAttributes::onSpawnPlacements);
        NeoForge.EVENT_BUS.register(ModDimensions.class);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.world.MarchRetrogen::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.world.MarchRetrogen::onLogin);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.gate.DrumRite::onLogout);
        NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.quest.DialogueSession::loggedOut);
        tk.darrow.tribalpower.compat.ModVersionCondition.register(modBus);
        tk.darrow.tribalpower.compat.ChocoboCompat.register();
        if (Boolean.getBoolean("tribalpower.marchSurvey")) NeoForge.EVENT_BUS.addListener(tk.darrow.tribalpower.verification.MarchSurvey::onServerStarted);
        modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> {
            // The flask is a tank in the hand: pipes, tanks and other mods fill it like any container.
            event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                    (stack, context) -> tk.darrow.tribalpower.item.SpiritFlaskItem.handler(stack),
                    ModItems.SPIRIT_FLASK.get(), ModItems.GREATER_SPIRIT_FLASK.get());
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    tk.darrow.tribalpower.camp.CampRegistry.TYPE.get(),(be,side)->be.hasInventory()?new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                            new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be,side==null?net.minecraft.core.Direction.UP:side)):null);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.SPIRIT_CISTERN.get(), (be, side) ->
                            tk.darrow.tribalpower.lattice.SidedFluidHandler.wrap(be, side, be.tank));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.PULSE_ADAPTER.get(), (be, side) -> be.handler);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.LATTICE_CONVERTER.get(), (be, side) -> be.handler);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.ECHO_STATION.get(), (be, side) ->
                            new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                            side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                    : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.ANCESTRAL_CACHE.get(), (be, side) -> new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                            side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                    : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.SONG_BENCH.get(), (be, side) ->
                            new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                                    side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                            : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            // Washing drinks 250 mB a cycle, so the mesh has to be something a bucket or a cistern can
            // actually reach; and the relays speak ItemHandler, where a hopper speaks WorldlyContainer.
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.RESONANCE_MESH.get(), (be, side) ->
                            tk.darrow.tribalpower.lattice.SidedFluidHandler.wrapInput(be, side, be.tank));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.RESONANCE_MESH.get(), (be, side) ->
                            new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                                    side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                            : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.STONE_FONT.get(), (be, side) ->
                            new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                                    side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                            : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.STONE_FONT.get(), (be, side) ->
                            tk.darrow.tribalpower.lattice.SidedFluidHandler.wrapInput(be, side, be.fluids));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    tk.darrow.tribalpower.device.DeviceRegistry.WORKSHOP.get(), (be, side) ->
                            "wind_snare".equals(be.kind())
                                    ? new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                                    side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                            : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side))
                                    : null);
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    tk.darrow.tribalpower.device.DeviceRegistry.SEAL_LOOM_TYPE.get(), (be, side) ->
                            new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                                    side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                            : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.RITE_PEDESTAL.get(), (be, side) ->
                            new tk.darrow.tribalpower.lattice.RedstoneItemHandler(be,
                                    side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(be)
                                            : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be, side)));
        });
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var fire = (net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE;
            fire.setFlammable(ModBlocks.MARCH_LOG.get(), 5, 5);
            fire.setFlammable(ModBlocks.MARCH_PLANKS.get(), 5, 20);
            fire.setFlammable(ModBlocks.MARCH_LEAVES.get(), 30, 60);
            fire.setFlammable(ModBlocks.MARCH_SAPLING.get(), 60, 100);
            fire.setFlammable(ModBlocks.MARCH_LEAF.get(), 60, 100);
            // The March's own woods burn like any wood, except Cinderwood, which grew up in the Ember Wastes.
            for (var entry : tk.darrow.tribalpower.world.MarchWoods.SETS.entrySet()) {
                var set = entry.getValue();
                if (entry.getKey() != tk.darrow.tribalpower.world.MarchWoods.Wood.CINDER) {
                    for (var log : java.util.List.of(set.log, set.wood, set.strippedLog, set.strippedWood)) fire.setFlammable(log.get(), 5, 5);
                    fire.setFlammable(set.planks.get(), 5, 20);
                    for (String part : new String[]{"stairs", "slab", "fence", "fence_gate"})
                        fire.setFlammable(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation
                                .fromNamespaceAndPath(MOD_ID, entry.getKey().id + "_" + part)), 5, 20);
                }
                fire.setFlammable(set.leaves.get(), 30, 60);
                fire.setFlammable(set.sapling.get(), 60, 100);
            }
            fire.setFlammable(tk.darrow.tribalpower.world.MarchTrees.WILLOW_STRAND.get(), 15, 100);
            fire.setFlammable(ModBlocks.SPIRIT_REED.get(), 60, 100);
            fire.setFlammable(ModBlocks.ECHO_BLOOM.get(), 60, 100);
            fire.setFlammable(ModBlocks.LEY_THISTLE.get(), 60, 100);
            LOGGER.info("Tribal Power — shamanic technomancy online");
        });
    }
}
