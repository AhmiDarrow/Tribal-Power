package tk.darrow.tribalpower.verification;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.*;
import tk.darrow.tribalpower.item.*;
import tk.darrow.tribalpower.world.ModDimensions;

@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class LatticeGameTests {
    @GameTest(template="empty")
    public static void codexHasValidItemsAndSpoilerSafeLanding(GameTestHelper h) {
        var entries=tk.darrow.tribalpower.guide.CodexEntries.ALL;
        h.assertTrue(entries.size()>=54 && !entries.getFirst().spoiler(),"Complete Codex needs a spoiler-safe landing");
        var ids=new java.util.HashSet<String>();
        for(var entry:entries) {
            h.assertTrue(ids.add(entry.id()),"Duplicate Codex id: "+entry.id());
            var id=net.minecraft.resources.ResourceLocation.parse("tribalpower:"+entry.icon());
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id),"Unknown Codex item: "+id);
            h.assertTrue(!entry.text().isBlank(),"Empty teaching: "+entry.id());
            if(!entry.picture().isEmpty())h.assertTrue(entry.spoiler(),"Creature pictures need spoiler protection");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void malformedCacheOwnerDoesNotDisableOtherPlayers(GameTestHelper h) {
        var id=java.util.UUID.randomUUID();var data=new tk.darrow.tribalpower.storage.DeepCacheSavedData();
        data.getOrCreateItems(id).set(0,new ItemStack(Items.DIAMOND,7));data.markVisitedMarch(id);
        var saved=data.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        var broken=new net.minecraft.nbt.CompoundTag();broken.putString("Id","invalid");broken.putString("Recovery","preserve this record");
        saved.getList("Players",10).add(0,broken.copy());saved.getList("Visited",10).add(0,broken.copy());
        var loaded=tk.darrow.tribalpower.storage.DeepCacheSavedData.load(saved,h.getLevel().registryAccess());
        h.assertTrue(loaded.getOrCreateItems(id).get(0).getCount()==7 && loaded.hasVisitedMarch(id),"Malformed owner must not disable valid inventories or visit flags");
        var roundtrip=loaded.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(roundtrip.getList("Players",10).contains(broken) && roundtrip.getList("Visited",10).contains(broken),"Unreadable records must remain recoverable after saving");h.succeed();
    }
    @GameTest(template="empty")
    public static void landingSafetyRejectsHazardsAndInvalidHeight(GameTestHelper h) {
        var floor=new BlockPos(6,1,2);var feet=h.absolutePos(floor.above());
        for(var block:java.util.List.of(Blocks.MAGMA_BLOCK,Blocks.CACTUS,Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE,Blocks.WITHER_ROSE,Blocks.POWDER_SNOW,Blocks.LAVA)) {
            h.setBlock(floor,block);
            h.assertTrue(tk.darrow.tribalpower.world.TravelSafety.hasHazard(h.getLevel(),feet),"Transport must reject hazard "+block);
        }
        h.setBlock(floor,Blocks.STONE);
        h.assertFalse(tk.darrow.tribalpower.world.TravelSafety.hasHazard(h.getLevel(),feet),"Clear stone landing should be usable");
        h.assertFalse(tk.darrow.tribalpower.world.TravelSafety.withinBounds(h.getLevel(),new BlockPos(0,h.getLevel().getMinBuildHeight(),0)),"Landing requires space for a floor");
        h.assertFalse(tk.darrow.tribalpower.world.TravelSafety.withinBounds(h.getLevel(),new BlockPos(0,h.getLevel().getMaxBuildHeight(),0)),"Landing above build height must be rejected");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void playerWaypointObeysLockCostAndCooldown(GameTestHelper h) {
        var player=h.makeMockServerPlayerInLevel();
        try {
            player.getAbilities().instabuild=false;
            var boat=h.spawn(net.minecraft.world.entity.EntityType.BOAT,new BlockPos(2,2,2));
            h.assertTrue(player.startRiding(boat,true),"Test player must be mounted");
            h.assertFalse(ModDimensions.travelThroughGate(player),"Gate must reject a mounted player before changing dimensions");
            player.stopRiding();boat.discard();
            var origin=h.absolutePos(new BlockPos(2,2,2));var floor=new BlockPos(6,1,2);var feet=h.absolutePos(floor.above());
            h.setBlock(floor,Blocks.STONE);player.setPos(origin.getX()+0.5,origin.getY(),origin.getZ()+0.5);
            var compass=new ItemStack(ModItems.WAYSTONE_COMPASS.get());var cell=PulseCellItem.createFilled(200);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,compass);player.getInventory().setItem(1,cell);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,compass,tag->{tag.putLong("Waypoint",feet.asLong());tag.putString("Dimension",h.getLevel().dimension().location().toString());});
            h.setBlock(6,1,3,Blocks.REDSTONE_BLOCK);
            compass.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(player.blockPosition().equals(origin) && PulseCellItem.getPulse(cell)==200,"Redstone lock must reject travel without charging");
            h.setBlock(6,1,3,Blocks.AIR);h.setBlock(floor,Blocks.MAGMA_BLOCK);
            compass.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(player.blockPosition().equals(origin) && PulseCellItem.getPulse(cell)==200,"Hazard rejection must preserve position and Pulse");
            h.setBlock(floor,Blocks.STONE);
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent> cancel = event -> {
                if (event.getEntity() == player) event.setCanceled(true);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try {
                var result=compass.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
                h.assertTrue(result.getResult()==net.minecraft.world.InteractionResult.FAIL && player.blockPosition().equals(origin) && PulseCellItem.getPulse(cell)==200,"Cancelled travel must fail and refund its charge");
                h.assertFalse(player.getCooldowns().isOnCooldown(compass.getItem()),"Cancelled travel must not start a cooldown");
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
            player.fallDistance=30;
            compass.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(player.blockPosition().equals(feet) && PulseCellItem.getPulse(cell)==180 && player.fallDistance==0,"Successful local travel must cost exactly 20 Pulse and clear fall damage");
            compass.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(PulseCellItem.getPulse(cell)==180,"Cooldown must block repeat charging");
            h.succeed();
        } finally { h.getLevel().getServer().getPlayerList().remove(player); }
    }
    @GameTest(template="empty")
    public static void pulseStorageRandomizedConservationAndCorruptLoads(GameTestHelper h) {
        var storage=new tk.darrow.tribalpower.api.pulse.PulseStorage(250);
        var random=new java.util.Random(4815162342L);
        int[] extremes={Integer.MIN_VALUE,-1,0,1,249,250,251,Integer.MAX_VALUE};
        for(int saved:extremes) {
            var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Pulse",saved);storage.load(tag);
            h.assertTrue(storage.getPulseStored()==Math.max(0,Math.min(250,saved)),"Saved Pulse must remain within physical capacity");
            for(int i=0;i<1000;i++) {
                int before=storage.getPulseStored();int amount=i%2==0?extremes[random.nextInt(extremes.length)]:random.nextInt(1000)-100;
                boolean insert=random.nextBoolean(),simulate=random.nextBoolean();
                int moved=insert?storage.insertPulse(amount,simulate):storage.extractPulse(amount,simulate);
                h.assertTrue(moved>=0 && moved<=Math.max(0,amount),"Transfers must never return negative or excessive amounts");
                h.assertTrue(storage.getPulseStored()==before+(simulate?0:insert?moved:-moved),"Every committed transfer must conserve Pulse; simulation must not mutate");
                h.assertTrue(storage.getPulseStored()>=0 && storage.getPulseStored()<=250,"Randomized operations must respect capacity");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void directBenchRemovalClearsInFlightWork(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.SONG_BENCH.get());
        var bench=at(h,pos,SongBenchBlockEntity.class);bench.setItem(0,new ItemStack(Items.COBBLESTONE));
        var tag=bench.saveWithoutMetadata(h.getLevel().registryAccess());tag.putInt("Progress",20);tag.putBoolean("Singing",true);
        bench.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(bench.removeItemNoUpdate(0).getCount()==1,"Direct removal must return the input");
        h.assertTrue(bench.getProgress()==0 && !bench.isSinging(),"Removing the input must cancel in-flight work");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=120)
    public static void invalidSavedStationWorkRecovers(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ECHO_SHATTER.get());power(h);
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var station=at(h,pos,EchoStationBlockEntity.class);station.setItem(0,new ItemStack(Items.COBBLESTONE));
        var recipe=tk.darrow.tribalpower.echo.ProcessingRecipes.find(h.getLevel(),station.station(),station.getItem(0));
        var tag=station.saveWithoutMetadata(h.getLevel().registryAccess());tag.putString("Recipe",recipe.id().toString());tag.putInt("Work",Integer.MAX_VALUE);
        station.loadWithComponents(tag,h.getLevel().registryAccess());
        h.runAfterDelay(85,()->{h.assertTrue(station.getItem(0).isEmpty() && station.getItem(1).is(ModItems.ECHO_SHARD.get()),"Invalid station progress must reset and finish normally");h.succeed();});
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void invalidSavedBenchProgressCannotOverflowAndStall(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.SONG_BENCH.get());power(h);
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var bench=at(h,pos,SongBenchBlockEntity.class);bench.setItem(0,new ItemStack(Items.COBBLESTONE));
        var tag=bench.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putBoolean("Singing",true);tag.putInt("Progress",Integer.MAX_VALUE);
        bench.loadWithComponents(tag,h.getLevel().registryAccess());
        h.runAfterDelay(45,()->{h.assertTrue(bench.getItem(0).is(ModItems.ECHO_SHARD.get()),"Invalid saved work must reset instead of overflowing into a permanent stall");h.succeed();});
    }
    @GameTest(template="empty")
    public static void staleChalkLinksCannotActivateUnconnectedTotems(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(2,2,2),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(4,2,2),ResonanceTotemBlockEntity.class);
        earth.addLink(h.absolutePos(new BlockPos(6,2,2)));
        h.assertFalse(tk.darrow.tribalpower.lattice.LatticeNetwork.isConductable(java.util.List.of(earth,fire)),"A missing third endpoint must not activate two unconnected totems");
        earth.addLink(fire.getBlockPos());
        h.assertTrue(tk.darrow.tribalpower.lattice.LatticeNetwork.collectChalkNetwork(h.getLevel(),earth).size()==1,"A replaced endpoint without a reciprocal link must not join the network");
        h.assertTrue(tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire),"Chalk must repair a one-sided stale link");
        h.assertTrue(tk.darrow.tribalpower.lattice.LatticeNetwork.collectChalkNetwork(h.getLevel(),earth).size()==2,"Repaired links must reconnect normally");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void totemBlockDeterminesAttunementAfterMalformedLoad(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var earth=at(h,pos,ResonanceTotemBlockEntity.class);
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Attunement","fire");
        earth.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(earth.getAttunement()==tk.darrow.tribalpower.api.pulse.Attunement.EARTH,"Earth block must not secretly provide a different attunement after loading");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=140)
    public static void partialFluidDeliverySurvivesSaveReload(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.FLUID_RELAY.get());power(h);
        h.setBlock(6,2,2,ModBlocks.SPIRIT_CISTERN.get());
        var sink=at(h,new BlockPos(6,2,2),SpiritCisternBlockEntity.class);
        sink.tank.fill(new FluidStack(Fluids.WATER,15950),IFluidHandler.FluidAction.EXECUTE);
        var relay=at(h,pos,WirelessRelayBlockEntity.class);
        var tag=new net.minecraft.nbt.CompoundTag();
        tag.putLong("Target",h.absolutePos(new BlockPos(6,2,2)).asLong());
        tag.putString("Dimension",h.getLevel().dimension().location().toString());
        tag.put("Pending",new FluidStack(Fluids.WATER,125).save(h.getLevel().registryAccess()));
        relay.loadWithComponents(tag,h.getLevel().registryAccess());
        h.runAfterDelay(40,()->{
            h.assertTrue(sink.tank.getFluidAmount()==16000,"Delivery must stop at tank capacity");
            var saved=relay.saveWithoutMetadata(h.getLevel().registryAccess());
            relay.loadWithComponents(saved,h.getLevel().registryAccess());
            int removed=sink.tank.drain(16000,IFluidHandler.FluidAction.EXECUTE).getAmount();
            h.runAfterDelay(45,()->{
                h.assertTrue(removed+sink.tank.getFluidAmount()==16075,"Saved remainder must survive reload and deliver exactly once");
                h.succeed();
            });
        });
    }
    @GameTest(template="empty")
    public static void pulseCellsRejectExtremeRequestsWithoutSimulationMutation(GameTestHelper h) {
        var cell=new ItemStack(ModItems.GREATER_PULSE_CELL.get());
        h.assertTrue(PulseCellItem.insertPulse(cell,Integer.MAX_VALUE,true)==1200 && PulseCellItem.getPulse(cell)==0,"Simulated fill must be bounded and read-only");
        PulseCellItem.insertPulse(cell,Integer.MAX_VALUE,false);
        h.assertTrue(PulseCellItem.extractPulse(cell,Integer.MAX_VALUE,true)==1200 && PulseCellItem.getPulse(cell)==1200,"Simulated drain must be read-only");
        h.assertTrue(PulseCellItem.extractPulse(cell,Integer.MIN_VALUE,false)==0 && PulseCellItem.insertPulse(cell,Integer.MIN_VALUE,false)==0,"Negative requests must not create charge");
        h.assertTrue(PulseCellItem.getPulse(cell)==1200,"Extreme requests must preserve stored charge");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void relayWithoutPulseDoesNotTouchSource(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ITEM_RELAY.get());
        h.setBlock(2,1,2,Blocks.CHEST);h.setBlock(6,2,2,Blocks.CHEST);
        var source=at(h,new BlockPos(2,1,2),ChestBlockEntity.class);
        var sink=at(h,new BlockPos(6,2,2),ChestBlockEntity.class);
        source.setItem(0,new ItemStack(Items.DIAMOND,64));
        var relay=at(h,pos,WirelessRelayBlockEntity.class);
        h.assertFalse(relay.bind(h.absolutePos(pos.below()),Direction.UP,h.getLevel().dimension().location().toString()),"A relay cannot target its own source");
        relay.bind(h.absolutePos(new BlockPos(6,2,2)),Direction.UP,h.getLevel().dimension().location().toString());
        h.runAfterDelay(45,()->{h.assertTrue(source.countItem(Items.DIAMOND)==64 && sink.isEmpty(),"Unpowered transfer must not extract or duplicate items");h.succeed();});
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void oversizedBenchInputIsNotCollapsedIntoOneOutput(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.SONG_BENCH.get());power(h);
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var bench=at(h,pos,SongBenchBlockEntity.class);
        bench.setItem(0,new ItemStack(Items.COBBLESTONE,32));bench.startSong();
        h.runAfterDelay(45,()->{
            h.assertTrue(bench.getItem(0).is(Items.COBBLESTONE) && bench.getItem(0).getCount()==32,"Malformed oversized input must remain recoverable, never become one output");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void oversizedBenchHandoffConservesRemainder(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.SONG_BENCH.get());h.setBlock(4,2,2,ModBlocks.SONG_BENCH.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var from=at(h,new BlockPos(2,2,2),SongBenchBlockEntity.class);
        var to=at(h,new BlockPos(4,2,2),SongBenchBlockEntity.class);
        from.setItem(0,new ItemStack(Items.COBBLESTONE,32));
        h.assertTrue(tk.darrow.tribalpower.lattice.LatticeNetwork.routeEchoItems(h.getLevel(),java.util.List.of(from,to),java.util.List.of()),"Handoff must deliver one item");
        h.assertTrue(from.getItem(0).getCount()==31 && to.getItem(0).getCount()==1,"Handoff must retain all excess items at source");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void fullTotemBuffersStillAllowConductorItemRouting(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(6,2,2,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(4,2,2),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(6,2,2),ResonanceTotemBlockEntity.class);
        tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire);
        earth.insertPulse(earth.getPulseCapacity(),false);fire.insertPulse(fire.getPulseCapacity(),false);
        h.setBlock(4,2,4,ModBlocks.SONG_BENCH.get());h.setBlock(6,2,4,ModBlocks.ANCESTRAL_CACHE.get());
        var bench=at(h,new BlockPos(4,2,4),SongBenchBlockEntity.class);
        var cache=at(h,new BlockPos(6,2,4),AncestralCacheBlockEntity.class);
        bench.setItem(0,new ItemStack(ModItems.MANIFESTED_INGOT.get()));
        h.runAfterDelay(45,()->{
            h.assertTrue(bench.isEmpty() && cache.countItem(ModItems.MANIFESTED_INGOT.get())==1,"Full charged buffers must not stall finished item routing");
            h.assertTrue(earth.getPulseStored()==earth.getPulseCapacity() && fire.getPulseStored()==fire.getPulseCapacity(),"Routing must not discard stored Pulse");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void conductorFeedStartsSongBench(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.SONG_BENCH.get());h.setBlock(4,2,2,ModBlocks.ANCESTRAL_CACHE.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var bench=at(h,new BlockPos(2,2,2),SongBenchBlockEntity.class);
        var cache=at(h,new BlockPos(4,2,2),AncestralCacheBlockEntity.class);
        cache.setItem(0,new ItemStack(Items.COBBLESTONE,2));
        h.assertTrue(tk.darrow.tribalpower.lattice.LatticeNetwork.routeEchoItems(h.getLevel(),java.util.List.of(bench),java.util.List.of(cache)),"Conductor must feed empty bench");
        h.assertTrue(bench.isSinging(),"Automated feed must start processing without a manual strike");
        h.assertTrue(bench.getItem(0).getCount()==1 && cache.getItem(0).getCount()==1,"Automated feed must conserve items");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=160)
    public static void stationUsesCapacityAcrossPartialOutputStacks(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ECHO_SHATTER.get());power(h);
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var station=at(h,pos,EchoStationBlockEntity.class);
        station.setItem(0,new ItemStack(Items.RAW_IRON));
        station.setItem(1,new ItemStack(ModItems.IRON_GRIT.get(),63));
        station.setItem(2,new ItemStack(ModItems.IRON_GRIT.get(),63));
        for(int i=3;i<9;i++)station.setItem(i,new ItemStack(Items.DIRT,64));
        h.runAfterDelay(110,()->{
            h.assertTrue(station.getItem(0).isEmpty(),"Recipe must use available capacity across output slots");
            h.assertTrue(station.getItem(1).getCount()==64 && station.getItem(2).getCount()==64,"Both outputs must fit without overstacking or loss");
            h.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void movedRelayRechecksRange(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ITEM_RELAY.get());
        h.setBlock(2,1,2,Blocks.CHEST);power(h);
        var source=at(h,new BlockPos(2,1,2),ChestBlockEntity.class);
        source.setItem(0,new ItemStack(Items.IRON_INGOT,16));
        var relay=at(h,pos,WirelessRelayBlockEntity.class);
        var tag=new net.minecraft.nbt.CompoundTag();
        tag.putLong("Target",h.absolutePos(pos).offset(40,0,0).asLong());
        tag.putString("Dimension",h.getLevel().dimension().location().toString());
        relay.loadWithComponents(tag,h.getLevel().registryAccess());
        h.runAfterDelay(40,()->{
            h.assertTrue(source.countItem(Items.IRON_INGOT)==16,"Moved local relay must retain source items");
            h.assertTrue(relay.status().equals(net.minecraft.network.chat.Component.translatable("message.tribalpower.relay.unlinked")),"Out-of-range saved link must be rejected before target access");
            h.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void bufferedFluidDoesNotRequireSourceTank(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.FLUID_RELAY.get());
        h.setBlock(6,2,2,ModBlocks.SPIRIT_CISTERN.get());power(h);
        var relay=at(h,pos,WirelessRelayBlockEntity.class);
        var tag=new net.minecraft.nbt.CompoundTag();
        tag.putLong("Target",h.absolutePos(new BlockPos(6,2,2)).asLong());
        tag.putString("Dimension",h.getLevel().dimension().location().toString());
        tag.put("Pending",new FluidStack(Fluids.WATER,125).save(h.getLevel().registryAccess()));
        relay.loadWithComponents(tag,h.getLevel().registryAccess());
        h.runAfterDelay(45,()->{
            h.assertTrue(at(h,new BlockPos(6,2,2),SpiritCisternBlockEntity.class).tank.getFluidAmount()==125,"Saved fluid must reach destination even after source removal");
            h.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void redstoneLocksCachedExternalCapabilities(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.SPIRIT_CISTERN.get());
        var tank=at(h,pos,SpiritCisternBlockEntity.class).tank;
        tank.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        h.setBlock(2,2,3,Blocks.REDSTONE_BLOCK);
        h.assertTrue(tank.drain(250,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Powered tank must refuse external draining");
        h.assertTrue(tank.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0,"Powered tank must refuse external filling");
        h.setBlock(6,2,2,ModBlocks.ANCESTRAL_CACHE.get());
        var cachePos=h.absolutePos(new BlockPos(6,2,2));
        var handler=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,cachePos,Direction.UP);
        handler.insertItem(0,new ItemStack(Items.IRON_INGOT,16),false);
        h.setBlock(6,2,3,Blocks.REDSTONE_BLOCK);
        h.assertTrue(handler.extractItem(0,16,false).isEmpty(),"Previously cached capability must obey live redstone lock");
        h.assertTrue(handler.insertItem(1,new ItemStack(Items.GOLD_INGOT),false).getCount()==1,"Powered cache must refuse insertion");
        h.setBlock(6,2,3,Blocks.AIR);
        h.assertTrue(handler.extractItem(0,16,false).getCount()==16,"Cache must resume without invalidating cached pipe references");
        h.succeed();
    }
    private static <T> T at(GameTestHelper h, BlockPos pos, Class<T> type) { return type.cast(h.getLevel().getBlockEntity(h.absolutePos(pos))); }
    private static void power(GameTestHelper h) {
        h.setBlock(3,2,4,ModBlocks.DRUMHEART.get());
        at(h,new BlockPos(3,2,4),DrumheartBlockEntity.class).insertPulse(1000,false);
    }
    @GameTest(template="empty", timeoutTicks=160)
    public static void resonanceUsesReusableCatalystAndRedstone(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,ModBlocks.PULSE_RESONATOR.get());
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var be=at(h,pos,PulseResonatorBlockEntity.class);
        h.assertFalse(PulseResonatorBlockEntity.isCatalyst(new ItemStack(Items.COAL)),"Coal must not power the lattice");
        be.acceptCatalyst(new ItemStack(ModItems.ECHO_SHARD.get()));
        h.runAfterDelay(45,()->{
            h.assertTrue(be.getPulseStored()>0,"Distinct voices must generate Pulse");
            h.assertTrue(be.getItem(0).getCount()==1,"Catalyst must not burn away");
            h.setBlock(2,2,3,Blocks.REDSTONE_BLOCK);
            int stored=be.getPulseStored();
            h.runAfterDelay(45,()->{ h.assertTrue(be.getPulseStored()==stored,"Redstone must pause generation");h.succeed(); });
        });
    }
    @GameTest(template="empty", timeoutTicks=200)
    public static void stationPreservesBatchAndStopsWhenFull(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ECHO_SHATTER.get());power(h);
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var be=at(h,pos,EchoStationBlockEntity.class);be.setItem(0,new ItemStack(Items.COBBLESTONE,32));
        for(int i=1;i<9;i++)be.setItem(i,new ItemStack(Items.DIRT,64));
        h.runAfterDelay(50,()->{
            h.assertTrue(be.getItem(0).getCount()==32,"Full output must not consume feed");
            be.setItem(1,ItemStack.EMPTY);
            h.runAfterDelay(90,()->{
                h.assertTrue(be.getItem(1).is(ModItems.ECHO_SHARD.get()),"Station must produce recipe output");
                h.assertTrue(be.getItem(0).getCount()+be.getItem(1).getCount()==32,"Batch item count must be conserved");
                h.succeed();
            });
        });
    }
    @GameTest(template="empty", timeoutTicks=200)
    public static void itemRelayMovesStacksAndPauses(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ITEM_RELAY.get());
        h.setBlock(2,1,2,Blocks.CHEST);h.setBlock(6,2,2,Blocks.CHEST);power(h);
        var source=at(h,new BlockPos(2,1,2),ChestBlockEntity.class);var target=at(h,new BlockPos(6,2,2),ChestBlockEntity.class);
        var relay=at(h,pos,WirelessRelayBlockEntity.class);relay.bind(h.absolutePos(new BlockPos(6,2,2)),Direction.UP,h.getLevel().dimension().location().toString());
        source.setItem(0,new ItemStack(Items.IRON_INGOT,32));h.setBlock(2,2,3,Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(40,()->{
            h.assertTrue(target.isEmpty(),"Powered relay must remain paused");h.setBlock(2,2,3,Blocks.AIR);
            h.setBlock(6,2,3,Blocks.REDSTONE_BLOCK);
            h.runAfterDelay(40,()->{
                h.assertTrue(target.isEmpty(),"Powered vanilla destination must lock transport");
                h.setBlock(6,2,3,Blocks.AIR);
                h.runAfterDelay(65,()->{h.assertTrue(source.isEmpty() && target.countItem(Items.IRON_INGOT)==32,"Wireless transfer must conserve stacks");h.succeed();});
            });
        });
    }
    @GameTest(template="empty", timeoutTicks=160)
    public static void astralFluidRelayCrossesDimensions(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ASTRAL_FLUID_RELAY.get());h.setBlock(2,1,2,ModBlocks.SPIRIT_CISTERN.get());power(h);
        var other=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        h.assertTrue(other!=null,"Nether must load in the GameTest fixture");
        // The receiver must be loaded independently: relays deliberately never load remote chunks.
        var endpoint=new BlockPos(100,150,100);other.setChunkForced(6,6,true);other.getChunk(6,6);other.setBlockAndUpdate(endpoint,ModBlocks.SPIRIT_CISTERN.get().defaultBlockState());
        var sink=(SpiritCisternBlockEntity)other.getBlockEntity(endpoint);sink.tank.setFluid(FluidStack.EMPTY);
        var source=at(h,new BlockPos(2,1,2),SpiritCisternBlockEntity.class);
        source.tank.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(at(h,pos,WirelessRelayBlockEntity.class).bind(endpoint,Direction.UP,other.dimension().location().toString()),"Astral relay must accept dimensional endpoint");
        h.runAfterDelay(100,()->{
            other.setChunkForced(6,6,false);
            h.assertTrue(source.tank.getFluidAmount()==0 && sink.tank.getFluidAmount()==1000,"Cross-dimensional fluid must be conserved: source="+source.tank.getFluidAmount()+", sink="+sink.tank.getFluidAmount()+", relay="+at(h,pos,WirelessRelayBlockEntity.class).status().getString());h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void lowerRelayRejectsDimensionAndGreaterCellHoldsCharge(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.ITEM_RELAY.get());
        h.assertFalse(at(h,pos,WirelessRelayBlockEntity.class).bind(new BlockPos(0,80,0),Direction.UP,"tribalpower:the_march"),"Local relay cannot cross dimensions");
        var cell=new ItemStack(ModItems.GREATER_PULSE_CELL.get());
        h.assertTrue(PulseCellItem.insertPulse(cell,1200,false)==1200 && PulseCellItem.getPulse(cell)==1200,"Greater cell must hold 1200 Pulse");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void adapterExportsButNeverImportsFE(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.PULSE_ADAPTER.get());power(h);
        var adapter=at(h,pos,PulseAdapterBlockEntity.class);
        h.assertTrue(adapter.handler.receiveEnergy(1000,false)==0,"FE must not convert back to Pulse");
        h.runAfterDelay(45,()->{
            h.assertTrue(adapter.handler.getEnergyStored()>0,"Adapter must convert nearby Pulse");
            h.setBlock(2,2,3,Blocks.REDSTONE_BLOCK);
            h.assertTrue(adapter.handler.extractEnergy(1000,false)==0,"Powered adapter must block external FE extraction");
            h.succeed();
        });
    }
}
