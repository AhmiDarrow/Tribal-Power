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
    public static void sideIoRespectsOwnershipAndPlayerMode(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, tk.darrow.tribalpower.camp.CampRegistry.DEVICES.get("offering_table").get());
        var device = (tk.darrow.tribalpower.camp.CampBlockEntity) h.getBlockEntity(pos);
        var player = VerificationPlayers.inLevel(h);
        try {
            device.setOwner(java.util.UUID.randomUUID());
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var before = device.sideIo().get(Direction.UP);
            h.assertFalse(tk.darrow.tribalpower.lattice.HasSideIo.cycle(player, device, Direction.UP),
                    "A configuration packet must not bypass another camp's ownership");
            h.assertTrue(device.sideIo().get(Direction.UP) == before, "Refused configuration must not mutate IO");
            device.setOwner(player.getUUID());
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            h.assertFalse(tk.darrow.tribalpower.lattice.HasSideIo.cycle(player, device, Direction.UP),
                    "Spectators cannot configure machines");
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            h.assertFalse(tk.darrow.tribalpower.lattice.HasSideIo.cycle(player, device, Direction.UP),
                    "Players without build permission cannot configure machines");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            h.assertTrue(tk.darrow.tribalpower.lattice.HasSideIo.cycle(player, device, Direction.UP),
                    "The owner can still configure the machine in survival");
            h.assertTrue(device.sideIo().get(Direction.UP) != before, "Allowed configuration must change the face");
            h.succeed();
        } finally { h.getLevel().getServer().getPlayerList().remove(player); }
    }

    /**
     * The Spirit Codex resolves: every category, item, block state, link, "next" and unlock points at something
     * real, the chapters a new player sees first are spoiler-free, and the facts other tests pin in Java are
     * still what the book says ({@link CodexFacts}).
     */
    @GameTest(template="empty")
    public static void codexIsWholeAndConsistent(GameTestHelper h) {
        var book=CodexFiles.english();
        h.assertTrue(book.entries().size()>=80,"The Codex should hold the whole mod, found "+book.entries().size()+" entries");
        h.assertTrue(!book.landing().isBlank(),"The Codex needs a landing page");
        var first=book.categories().getFirst();
        h.assertTrue(!first.spoiler() && book.in(first.id()).stream().noneMatch(e->e.spoiler()),"The first chapter must be spoiler-free: "+first.id());
        var link=java.util.regex.Pattern.compile("\\[[^\\]]+\\]\\(([a-z0-9_]+)\\)");
        var itemRef=java.util.regex.Pattern.compile("\\{item:([a-z0-9_.:/-]+)\\}");
        java.util.function.Consumer<String> item=id->h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(tk.darrow.tribalpower.client.codex.CodexBook.itemId(id)),"Unknown Codex item: "+id);
        java.util.function.BiConsumer<String,String> text=(where,body)->{
            var m=link.matcher(body);
            while(m.find())h.assertTrue(book.byId().containsKey(m.group(1)),where+" links to a missing entry: "+m.group(1));
            var i=itemRef.matcher(body);
            while(i.find())item.accept(i.group(1));
        };
        for(var c:book.categories()) {
            item.accept(c.icon());
            h.assertTrue(!book.in(c.id()).isEmpty(),"Empty chapter: "+c.id());
            text.accept("chapter "+c.id(),c.description());
        }
        text.accept("landing",book.landing());
        for(var e:book.entries()) {
            h.assertTrue(book.category(e.category())!=null,e.id()+" sits in a missing chapter "+e.category());
            h.assertTrue(!e.name().isBlank() && !e.pages().isEmpty(),"Empty entry: "+e.id());
            e.items().forEach(item);
            if(!e.next().isEmpty())h.assertTrue(book.byId().containsKey(e.next()),e.id()+" continues to a missing entry "+e.next());
            if(!e.unlock().isEmpty())h.assertTrue(e.spoiler() && e.unlock().matches("(tribe:[a-z]+|tablet:\\d+)"),e.id()+" has a malformed unlock "+e.unlock());
            for(var page:e.pages()) {
                text.accept(e.id(),page.text());
                switch(page) {
                    case tk.darrow.tribalpower.client.codex.CodexBook.Spotlight s -> item.accept(s.item());
                    case tk.darrow.tribalpower.client.codex.CodexBook.Recipe r -> item.accept(r.item());
                    case tk.darrow.tribalpower.client.codex.CodexBook.Image i -> h.assertTrue(
                            LatticeGameTests.class.getClassLoader().getResource("assets/tribalpower/textures/gui/codex/"+i.image()+".png")!=null,e.id()+" shows a missing picture "+i.image());
                    case tk.darrow.tribalpower.client.codex.CodexBook.Pattern pattern -> h.assertTrue(
                            java.util.Set.of("stone_font","listening_pit","rite_circle","voice_ring","shatter_array","way_gate","far_gate").contains(pattern.pattern()),e.id()+" draws an unknown pattern "+pattern.pattern());
                    case tk.darrow.tribalpower.client.codex.CodexBook.Scene scene -> {
                        h.assertTrue(!scene.steps().isEmpty(),e.id()+" has an empty scene");
                        for(var step:scene.steps()) {
                            h.assertTrue(!step.caption().isBlank(),e.id()+" has a scene step with no caption");
                            text.accept(e.id(),step.caption());
                            for(var placed:step.place()) {
                                try {
                                    net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),
                                            placed.state().contains(":")?placed.state():"tribalpower:"+placed.state(),false);
                                } catch(com.mojang.brigadier.exceptions.CommandSyntaxException bad) {
                                    h.fail(e.id()+" places an unknown block "+placed.state());
                                }
                            }
                            if(step.use()!=null)item.accept(step.use().item());
                        }
                    }
                    default -> { }
                }
            }
        }
        CodexFacts.check(h,book);
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
        var player=VerificationPlayers.inLevel(h);
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
        var bench=at(h,pos,SongBenchBlockEntity.class);bench.setItem(0,new ItemStack(Items.STONE));
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
        var station=at(h,pos,EchoStationBlockEntity.class);station.setItem(0,new ItemStack(Items.STONE));
        var recipe=tk.darrow.tribalpower.echo.ProcessingRecipes.find(h.getLevel(),station.station(),station.getItem(0));
        var tag=station.saveWithoutMetadata(h.getLevel().registryAccess());tag.putString("Recipe",recipe.id().toString());tag.putInt("Work",Integer.MAX_VALUE);
        station.loadWithComponents(tag,h.getLevel().registryAccess());
        h.runAfterDelay(85,()->{h.assertTrue(station.getItem(0).isEmpty() && station.getItem(1).is(ModItems.ECHO_SHARD.get()),"Invalid station progress must reset and finish normally");h.succeed();});
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void invalidSavedBenchProgressCannotOverflowAndStall(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.SONG_BENCH.get());power(h);
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var bench=at(h,pos,SongBenchBlockEntity.class);bench.setItem(0,new ItemStack(Items.STONE));
        var tag=bench.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putBoolean("Singing",true);tag.putInt("Progress",Integer.MAX_VALUE);
        bench.loadWithComponents(tag,h.getLevel().registryAccess());
        h.runAfterDelay(45,()->{h.assertTrue(bench.getItem(0).is(ModItems.ECHO_SHARD.get()),"Invalid saved work must reset instead of overflowing into a permanent stall");h.succeed();});
    }
    @GameTest(template="empty")
    public static void seatingABenchItemStartsTheSong(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.SONG_BENCH.get());power(h);
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var bench=at(h,pos,SongBenchBlockEntity.class);
        bench.setItem(0,new ItemStack(Items.STONE));
        h.assertTrue(bench.isSinging(),"A hopper seating a processable item starts the song");
        h.succeed();
    }
    /**
     * The Lattice Converter takes FE, will not give it back, and puts Pulse into the lattice.
     *
     * <p>The one-way handler is what keeps it from feeding the Harmonic Energizer that fed it.
     */
    @GameTest(template="empty")
    public static void theConverterTurnsFeIntoLatticePulse(GameTestHelper h) {
        BlockPos at=new BlockPos(2,2,2), sink=new BlockPos(4,2,2);
        h.setBlock(at, tk.darrow.tribalpower.block.ModBlocks.LATTICE_CONVERTER.get());
        h.setBlock(sink, tk.darrow.tribalpower.block.ModBlocks.DRUMHEART.get());
        var converter=at(h,at,tk.darrow.tribalpower.blockentity.LatticeConverterBlockEntity.class);
        var drum=at(h,sink,DrumheartBlockEntity.class);

        h.assertTrue(converter.handler.canReceive() && !converter.handler.canExtract(),
                "The Converter takes FE and never gives it back");
        h.assertTrue(converter.handler.extractEnergy(10_000,false)==0,"FE must not come back out");

        int fed=converter.handler.receiveEnergy(40_000,false);
        h.assertTrue(fed>0,"The Converter must accept FE, took "+fed);
        int before=drum.getPulseStored();
        // Run it long enough for its once-a-second beat to land whatever the world time is.
        h.onEachTick(()->tk.darrow.tribalpower.blockentity.LatticeConverterBlockEntity.tick(
                h.getLevel(), h.absolutePos(at), h.getBlockState(at), converter));
        h.succeedWhen(()->{
            h.assertTrue(drum.getPulseStored()>before,
                    "The Converter must push Pulse into the lattice; drum still holds "+drum.getPulseStored());
            h.assertTrue(converter.getEnergy()<fed,"Making Pulse must cost it FE");
        });
    }

    /**
     * Drums crowd each other out, so a shed full of them is not an answer to everything.
     *
     * <p>Two drums on a one-second clock paid 48 a second forever and beat every other generator in
     * the mod. Only {@link DrumheartBlockEntity#MAX_PER_ZONE} in any zone earn now; the rest still
     * beat and still sound, but pay nothing.
     */
    @GameTest(template="empty")
    public static void onlyTwoDrumsInAZoneEarn(GameTestHelper h) {
        BlockPos[] crowded={new BlockPos(1,2,1),new BlockPos(2,2,1),new BlockPos(3,2,1),new BlockPos(4,2,1)};
        for(BlockPos p:crowded) h.setBlock(p, tk.darrow.tribalpower.block.ModBlocks.DRUMHEART.get());
        int paying=0;
        for(BlockPos p:crowded) if(at(h,p,DrumheartBlockEntity.class).paysInZone()) paying++;
        h.assertTrue(paying==DrumheartBlockEntity.MAX_PER_ZONE,
                "Four drums in one zone: exactly "+DrumheartBlockEntity.MAX_PER_ZONE
                        +" must earn, "+paying+" did");
        // And the ones that are crowded out really do earn nothing, not merely less.
        int earned=0;
        for(BlockPos p:crowded) {
            var drum=at(h,p,DrumheartBlockEntity.class);
            if(!drum.paysInZone()) earned+=drum.onRedstonePulse();
        }
        h.assertTrue(earned==0,"A crowded drum must pay nothing, paid "+earned);
        h.succeed();
    }

    /** Spread them past a zone apart and every one of them earns again. */
    @GameTest(template="empty")
    public static void drumsSpreadBeyondAZoneAllEarn(GameTestHelper h) {
        int gap=DrumheartBlockEntity.ZONE_RADIUS+1;
        BlockPos[] spread={new BlockPos(1,2,1),new BlockPos(1+gap,2,1)};
        for(BlockPos p:spread) h.setBlock(p, tk.darrow.tribalpower.block.ModBlocks.DRUMHEART.get());
        for(BlockPos p:spread)
            h.assertTrue(at(h,p,DrumheartBlockEntity.class).paysInZone(),
                    "A drum more than a zone away from the others must still earn");
        h.succeed();
    }

    /**
     * Every catalyst tier has to be a real upgrade, in output and in how long it lasts.
     *
     * <p>Rank was an addend: at six voices the four catalysts paid 48, 60, 72 and 84, so walking a
     * Resonant Core out to the ring bought a third more than the shard already sitting in it. It
     * multiplies now, and each tier must still outlast the one below.
     */
    @GameTest(template="empty")
    public static void everyCatalystTierIsARealUpgrade(GameTestHelper h) {
        for(int voices=2;voices<=6;voices++) {
            int previous=0;
            for(int rank=1;rank<=4;rank++) {
                int gain=PulseResonatorBlockEntity.gainFor(voices,rank);
                h.assertTrue(gain>previous,
                        "At "+voices+" voices, rank "+rank+" must beat rank "+(rank-1)
                                +"; made "+gain+" against "+previous);
                // A tier is only worth fetching if it is worth appreciably more, not a few Pulse.
                h.assertTrue(rank==1 || gain>=previous*5/4,
                        "At "+voices+" voices, rank "+rank+" must be at least a quarter better than "
                                +previous+", made "+gain);
                previous=gain;
            }
        }
        // No catalyst at all is no Pulse at all, whatever the voices.
        h.assertTrue(PulseResonatorBlockEntity.gainFor(6,0)==0,"No catalyst must make nothing");
        h.succeed();
    }

    /**
     * A built Resonator has to be worth building.
     *
     * <p>It paid {@code 2 * voices + 2 * rank}: twenty a second for six voices in a ring under a
     * Resonant Core. A Drumheart on a one-second clock pays {@link DrumheartBlockEntity#ON_TEMPO}
     * a beat, so two drums and a repeater loop beat the whole ritual, and nothing here noticed
     * because no test had ever read the Resonator's output.
     */
    @GameTest(template="empty")
    public static void aBuiltResonatorOutEarnsABankOfDrums(GameTestHelper h) {
        int full = PulseResonatorBlockEntity.gainFor(6, 4);          // six voices, ringed, top catalyst
        int twoDrums = 2 * DrumheartBlockEntity.ON_TEMPO;            // both on a 20 tick clock
        h.assertTrue(full > twoDrums,
                "A full six-voice ring under a Resonant Core must beat two clocked drums; made "
                        + full + " against " + twoDrums);
        // Voices have to multiply, or the sixth is not worth fetching: the step from five to six
        // must be bigger than the step from two to three.
        int lateStep = PulseResonatorBlockEntity.gainFor(6, 4) - PulseResonatorBlockEntity.gainFor(5, 4);
        int earlyStep = PulseResonatorBlockEntity.gainFor(3, 4) - PulseResonatorBlockEntity.gainFor(2, 4);
        h.assertTrue(lateStep > earlyStep,
                "Voices must multiply, not add: late step " + lateStep + ", early step " + earlyStep);
        // And the ring has to pay for itself, since an unarranged heap is capped.
        h.assertTrue(PulseResonatorBlockEntity.gainFor(6, 4)
                        > PulseResonatorBlockEntity.gainFor(PulseResonatorBlockEntity.UNARRANGED_VOICES, 4),
                "Arranging the totems in a ring must be worth doing");
        h.succeed();
    }

    /**
     * The catalyst is not permanent. It was, which made a built Resonator free forever; it now wears
     * by the Pulse it has actually delivered, so a bigger resonator eats catalysts faster.
     */
    @GameTest(template="empty")
    public static void aResonatorCatalystWearsOutAndCrumbles(GameTestHelper h) {
        BlockPos pos=new BlockPos(2,2,2);
        h.setBlock(pos, tk.darrow.tribalpower.block.ModBlocks.PULSE_RESONATOR.get());
        var resonator=at(h,pos,PulseResonatorBlockEntity.class);
        resonator.acceptCatalyst(new ItemStack(ModItems.ECHO_SHARD.get()));
        h.assertTrue(resonator.catalystCount()==1,"The shard must seat");
        h.assertTrue(resonator.catalystLife()>0.99F,"A fresh catalyst is whole");
        int life=PulseResonatorBlockEntity.endurance(1);
        resonator.wearCatalyst(life/2);
        h.assertTrue(Math.abs(resonator.catalystLife()-0.5F)<0.02F,
                "Half its Pulse spent is half its life gone, saw "+resonator.catalystLife());
        resonator.wearCatalyst(life/2+1);
        h.assertTrue(resonator.catalystCount()==0,"A spent catalyst must crumble, not linger");
        // Every rank has to have a life, and a better catalyst has to last longer.
        int previous=0;
        for(int rank=1;rank<=4;rank++) {
            int endurance=PulseResonatorBlockEntity.endurance(rank);
            h.assertTrue(endurance>previous,"Rank "+rank+" must outlast rank "+(rank-1));
            previous=endurance;
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void hoppersCannotStealAResonatorCatalyst(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.PULSE_RESONATOR.get());
        var resonator=at(h,pos,PulseResonatorBlockEntity.class);
        resonator.acceptCatalyst(new ItemStack(ModItems.ECHO_SHARD.get()));
        h.assertTrue(resonator.getSlotsForFace(Direction.DOWN).length==0
                        && !resonator.canTakeItemThroughFace(0,resonator.getItem(0),Direction.DOWN)
                        && !resonator.canPlaceItemThroughFace(0,new ItemStack(ModItems.ECHO_SHARD.get()),Direction.UP),
                "Hoppers have no face into a Resonator");
        h.succeed();
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
        bench.setItem(0,new ItemStack(Items.STONE,32));bench.startSong();
        h.runAfterDelay(45,()->{
            h.assertTrue(bench.getItem(0).is(Items.STONE) && bench.getItem(0).getCount()==32,"Malformed oversized input must remain recoverable, never become one output");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void oversizedBenchHandoffConservesRemainder(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.SONG_BENCH.get());h.setBlock(4,2,2,ModBlocks.SONG_BENCH.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var from=at(h,new BlockPos(2,2,2),SongBenchBlockEntity.class);
        var to=at(h,new BlockPos(4,2,2),SongBenchBlockEntity.class);
        from.setItem(0,new ItemStack(Items.STONE,32));
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
    public static void conductorRefundsAHornWhenTotemsAreFull(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(6,2,2,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(4,2,2),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(6,2,2),ResonanceTotemBlockEntity.class);
        tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire);
        earth.insertPulse(earth.getPulseCapacity(),false);fire.insertPulse(fire.getPulseCapacity(),false);
        h.setBlock(2,2,4,tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
        var horn=at(h,new BlockPos(2,2,4),tk.darrow.tribalpower.generator.EmberHornBlockEntity.class);
        horn.insertPulse(200,false);
        int before=horn.getPulseStored();
        var conductor=at(h,pos,LatticeConductorBlockEntity.class);
        for(int i=0;i<LatticeConductorBlockEntity.TICK_INTERVAL;i++)
            LatticeConductorBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),conductor);
        h.assertTrue(horn.getPulseStored()==before,"Unused extract must return to a horn, stored "+horn.getPulseStored());
        h.succeed();
    }
    /** A Cairn eats the same generators the Conductor draws on, so the Conductor must be able to read it back. */
    @GameTest(template="empty")
    public static void conductorLiftsACairnsHeldBeatOntoTheLattice(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(2,2,3,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(2,2,3),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(2,2,4),ResonanceTotemBlockEntity.class);
        tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire);
        h.setBlock(2,2,5,ModBlocks.PULSE_CAIRN.get());
        var cairn=at(h,new BlockPos(2,2,5),PulseCairnBlockEntity.class);
        cairn.insertPulse(500,false);
        h.assertTrue(cairn.getPulseStored()==500,"Cairn must hold what it was given, holds "+cairn.getPulseStored());
        var conductor=at(h,pos,LatticeConductorBlockEntity.class);
        for(int i=0;i<LatticeConductorBlockEntity.TICK_INTERVAL;i++)
            LatticeConductorBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),conductor);
        h.assertTrue(conductor.getLastPulsePushed()==LatticeConductorBlockEntity.PUSH_PER_CYCLE,
                "A Cairn alone must feed the lattice, pushed "+conductor.getLastPulsePushed());
        h.assertTrue(earth.getPulseStored()+fire.getPulseStored()==LatticeConductorBlockEntity.PUSH_PER_CYCLE,
                "Cairn Pulse must land in the totems, earth "+earth.getPulseStored()+" fire "+fire.getPulseStored());
        h.assertTrue(cairn.getPulseStored()==500-LatticeConductorBlockEntity.PUSH_PER_CYCLE,
                "Cairn must be drawn down by exactly what moved, holds "+cairn.getPulseStored());
        h.succeed();
    }
    /** Pulse a station has already claimed is not the Conductor's to take back. */
    @GameTest(template="empty")
    public static void conductorDoesNotRobAStationsOwnBuffer(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(2,2,3,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(2,2,3),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(2,2,4),ResonanceTotemBlockEntity.class);
        tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire);
        h.setBlock(2,2,5,ModBlocks.RESONANCE_MESH.get());
        var mesh=at(h,new BlockPos(2,2,5),ResonanceMeshBlockEntity.class);
        mesh.insertPulse(mesh.getPulseCapacity(),false);
        int held=mesh.getPulseStored();
        h.assertTrue(held>0,"Listening Pit must hold its buffer for the test");
        var conductor=at(h,pos,LatticeConductorBlockEntity.class);
        for(int i=0;i<LatticeConductorBlockEntity.TICK_INTERVAL;i++)
            LatticeConductorBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),conductor);
        h.assertTrue(mesh.getPulseStored()==held,"A station's claimed Pulse must survive, holds "+mesh.getPulseStored());
        h.assertTrue(conductor.getLastPulsePushed()==0,"Nothing drawable means nothing pushed");
        h.assertTrue(!conductor.isLatticeFull(),"Empty totems with no source is dry, not full");
        h.succeed();
    }
    /** A charged camp with full totems is not a dry one: the Conductor must not cry "No Pulse" at it. */
    @GameTest(template="empty")
    public static void fullLatticeIsReportedAsFullNotAsNoPulse(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(2,2,3,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(2,2,3),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(2,2,4),ResonanceTotemBlockEntity.class);
        tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire);
        earth.insertPulse(earth.getPulseCapacity(),false);fire.insertPulse(fire.getPulseCapacity(),false);
        h.setBlock(2,2,5,ModBlocks.PULSE_RESONATOR.get());
        var resonator=at(h,new BlockPos(2,2,5),PulseResonatorBlockEntity.class);
        resonator.insertPulse(resonator.getPulseCapacity(),false);
        var conductor=at(h,pos,LatticeConductorBlockEntity.class);
        for(int i=0;i<LatticeConductorBlockEntity.TICK_INTERVAL;i++)
            LatticeConductorBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),conductor);
        h.assertTrue(conductor.getNetworkSize()==2,"Conductor must see the linked pair, saw "+conductor.getNetworkSize());
        h.assertTrue(conductor.getLastPulsePushed()==0,"Full buffers accept nothing, pushed "+conductor.getLastPulsePushed());
        h.assertTrue(conductor.getLastSourceAvailable()>0,
                "A charged generator in range must still read as available, saw "+conductor.getLastSourceAvailable());
        h.assertTrue(conductor.isLatticeFull(),"A charged camp with full totems must report full, not dry");
        h.assertTrue(resonator.getPulseStored()==resonator.getPulseCapacity(),
                "A full lattice must not draw Pulse it can only hand straight back, source at "+resonator.getPulseStored());
        var lines=conductor.diagnose(h.getLevel(),h.absolutePos(pos)).toString();
        h.assertTrue(lines.contains("conductor.buffers_full") && !lines.contains("conductor.no_pulse"),
                "Diagnosis must name the full lattice, said "+lines);
        h.succeed();
    }
    /** A camp with room but no charged generator is the genuine dry case, and must still say so. */
    @GameTest(template="empty")
    public static void anEmptyCampStillReportsNoPulse(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(2,2,3,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(2,2,3),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(2,2,4),ResonanceTotemBlockEntity.class);
        tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire);
        var conductor=at(h,pos,LatticeConductorBlockEntity.class);
        for(int i=0;i<LatticeConductorBlockEntity.TICK_INTERVAL;i++)
            LatticeConductorBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),conductor);
        h.assertTrue(!conductor.isLatticeFull(),"An empty camp is dry, not full");
        var lines=conductor.diagnose(h.getLevel(),h.absolutePos(pos)).toString();
        h.assertTrue(lines.contains("conductor.no_pulse") && !lines.contains("conductor.buffers_full"),
                "Diagnosis must still name a dry camp, said "+lines);
        h.succeed();
    }
    /** The plain camp wiring of the Codex scene: a live generator by the Conductor, two chalk-linked totems. */
    @GameTest(template="empty")
    public static void conductorMovesGeneratorPulseOntoTheLattice(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.LATTICE_CONDUCTOR.get());
        h.setBlock(4,2,2,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        h.setBlock(6,2,2,ModBlocks.RESONANCE_TOTEM_FIRE.get());
        var earth=at(h,new BlockPos(4,2,2),ResonanceTotemBlockEntity.class);
        var fire=at(h,new BlockPos(6,2,2),ResonanceTotemBlockEntity.class);
        tk.darrow.tribalpower.lattice.LatticeNetwork.linkTotems(earth,fire);
        h.setBlock(2,2,4,tk.darrow.tribalpower.generator.GeneratorRegistry.EMBER_HORN.get());
        var horn=at(h,new BlockPos(2,2,4),tk.darrow.tribalpower.generator.EmberHornBlockEntity.class);
        horn.insertPulse(200,false);
        var conductor=at(h,pos,LatticeConductorBlockEntity.class);
        for(int i=0;i<LatticeConductorBlockEntity.TICK_INTERVAL;i++)
            LatticeConductorBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),conductor);
        h.assertTrue(conductor.getNetworkSize()==2,"Conductor must see both totems, saw "+conductor.getNetworkSize());
        h.assertTrue(conductor.getLastPulsePushed()==LatticeConductorBlockEntity.PUSH_PER_CYCLE,
                "Conductor must push a full cycle from a live generator, pushed "+conductor.getLastPulsePushed());
        h.assertTrue(earth.getPulseStored()+fire.getPulseStored()==LatticeConductorBlockEntity.PUSH_PER_CYCLE,
                "Pushed Pulse must land in totem buffers, earth "+earth.getPulseStored()+" fire "+fire.getPulseStored());
        h.assertTrue(horn.getPulseStored()==200-LatticeConductorBlockEntity.PUSH_PER_CYCLE,
                "Generator must be drawn down by exactly what moved, stored "+horn.getPulseStored());
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void aBondedRelayDoesNotFallBackToATunerMark(GameTestHelper h) {
        h.setBlock(2,1,2,Blocks.STONE);
        h.setBlock(2,2,2,ModBlocks.ITEM_RELAY.get());
        h.setBlock(6,2,2,Blocks.CHEST);
        var relay=at(h,new BlockPos(2,2,2),WirelessRelayBlockEntity.class);
        var chestPos=h.absolutePos(new BlockPos(6,2,2));
        h.assertTrue(relay.bind(chestPos,Direction.UP,h.getLevel().dimension().location().toString()),"Tuner mark seats");
        relay.setItem(0,new ItemStack(Items.DIAMOND));
        h.succeedWhen(()->{
            WirelessRelayBlockEntity.tick(h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),h.getBlockState(new BlockPos(2,2,2)),relay);
            h.assertTrue(relay.status().equals(net.minecraft.network.chat.Component.translatable("message.tribalpower.relay.unlinked")),
                    "A Bonded plate waits for its pair instead of dumping into the leftover mark");
        });
    }
    @GameTest(template="empty")
    public static void conductorFeedStartsSongBench(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.SONG_BENCH.get());h.setBlock(4,2,2,ModBlocks.ANCESTRAL_CACHE.get());
        h.setBlock(2,2,4,ModBlocks.RESONANCE_TOTEM_EARTH.get());
        var bench=at(h,new BlockPos(2,2,2),SongBenchBlockEntity.class);
        var cache=at(h,new BlockPos(4,2,2),AncestralCacheBlockEntity.class);
        cache.setItem(0,new ItemStack(Items.STONE,2));
        h.assertTrue(tk.darrow.tribalpower.lattice.LatticeNetwork.routeEchoItems(h.getLevel(),java.util.List.of(bench),java.util.List.of(cache)),"Conductor must feed empty bench");
        h.assertTrue(bench.isSinging(),"Automated feed must start processing without a manual strike");
        h.assertTrue(bench.getItem(0).getCount()==1 && cache.getItem(0).getCount()==1,"Automated feed must conserve items");
        h.succeed();
    }
    /** The Gate Rite: a fair, playable pattern, identical on both sides, and a server that is not easily fooled. */
    @GameTest(template="empty")
    public static void theGateRiteIsFairAndHardToFool(GameTestHelper h) {
        var rite=tk.darrow.tribalpower.gate.DrumRite.pattern(1234L);
        var again=tk.darrow.tribalpower.gate.DrumRite.pattern(1234L);
        h.assertTrue(rite.equals(again),"Client and server must build the same pattern from a seed");
        for(long seed=0;seed<tk.darrow.tribalpower.gate.DrumRite.trackCount();seed++) {
            var p=tk.darrow.tribalpower.gate.DrumRite.pattern(seed);
            h.assertTrue(p.playMs()>=20000 && p.playMs()<=30000,"A rite lasts 20 to 30 seconds, got "+p.playMs());
            h.assertTrue(p.notes().size()>=30,"A rite has a real rhythm, got "+p.notes().size()+" beats");
            for(int i=1;i<p.notes().size();i++)
                h.assertTrue(p.notes().get(i).timeMs()-p.notes().get(i-1).timeMs()>=100,"Beats are never closer than 100 ms");
        }
        h.assertTrue(tk.darrow.tribalpower.gate.DrumRite.accuracy(40,120,40)<tk.darrow.tribalpower.gate.DrumRite.PASS,"Mashing all four drums on every beat does not pass");
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.GATE_DRUM.get());
        var player=VerificationPlayers.inLevel(h);
        var abs=h.absolutePos(pos);
        player.moveTo(abs.getX()+1.5,abs.getY(),abs.getZ()+0.5);
        int total=rite.notes().size();
        // Too soon: a result that arrives before the rhythm could have been played is refused.
        tk.darrow.tribalpower.gate.DrumRite.beginAt(player,abs,1234L,20);
        h.assertTrue(!tk.darrow.tribalpower.gate.DrumRite.finish(player,new tk.darrow.tribalpower.gate.DrumRite.Result(abs,1234L,total,0,false)),"An early result is refused");
        // On time but off the beat: fails, and the drum keeps its Pulse.
        long ticks=rite.endMs()/50;
        tk.darrow.tribalpower.gate.DrumRite.beginAt(player,abs,1234L,ticks);
        h.assertTrue(!tk.darrow.tribalpower.gate.DrumRite.finish(player,new tk.darrow.tribalpower.gate.DrumRite.Result(abs,1234L,total/2,0,false)),"Half the beats does not open the gate");
        // A result without a rite, or for another seed, is ignored.
        h.assertTrue(!tk.darrow.tribalpower.gate.DrumRite.finish(player,new tk.darrow.tribalpower.gate.DrumRite.Result(abs,1234L,total,0,false)),"No rite, no gate");
        tk.darrow.tribalpower.gate.DrumRite.beginAt(player,abs,99L,ticks);
        h.assertTrue(!tk.darrow.tribalpower.gate.DrumRite.finish(player,new tk.darrow.tribalpower.gate.DrumRite.Result(abs,1234L,total,0,false)),"The seed must match");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void aStoneFontTakesWaterOnAnInputFace(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.STONE_FONT.get());
        var font=at(h,pos,StoneFontBlockEntity.class);
        var sided=tk.darrow.tribalpower.lattice.SidedFluidHandler.wrap(font,Direction.NORTH,font.fluids);
        h.assertTrue(sided.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==250,
                "A bucket face must be able to fill a new font");
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
        var be=at(h,pos,EchoStationBlockEntity.class);be.setItem(0,new ItemStack(Items.STONE,32));
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
    @GameTest(template="empty")
    public static void tunerMarksAMachineNotThePlate(GameTestHelper h) {
        h.setBlock(2,1,2,Blocks.STONE);
        h.setBlock(2,2,2,ModBlocks.ITEM_RELAY.get());
        h.setBlock(6,2,2,Blocks.CHEST);
        var player=VerificationPlayers.inLevel(h);
        var tuner=new ItemStack(ModItems.LATTICE_TUNER.get());
        var relayPos=h.absolutePos(new BlockPos(2,2,2));
        var chestPos=h.absolutePos(new BlockPos(6,2,2));
        tuner.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,tuner,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(relayPos),Direction.NORTH,relayPos,false)));
        h.assertTrue(!tuner.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("Endpoint"),
                "A tuner does not mark the plate itself");
        tuner.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,tuner,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chestPos),Direction.UP,chestPos,false)));
        h.assertTrue(tuner.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("Endpoint"),
                "A tuner marks a machine face");
        tuner.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,tuner,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(relayPos),Direction.NORTH,relayPos,false)));
        var saved=at(h,new BlockPos(2,2,2),WirelessRelayBlockEntity.class).saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.contains("Target") && saved.getLong("Target")==chestPos.asLong(),"The plate binds the marked chest, not itself");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void adapterExportsButNeverImportsFE(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.PULSE_ADAPTER.get());power(h);
        var adapter=at(h,pos,PulseAdapterBlockEntity.class);
        h.assertTrue(adapter.pulseRate()==PulseAdapterBlockEntity.RATE,"Unranked adapter converts 20 Pulse/s");
        tk.darrow.tribalpower.item.MachineRank.apply(adapter,1);
        h.assertTrue(adapter.pulseRate()==tk.darrow.tribalpower.item.MachineRank.scalePulse(adapter,PulseAdapterBlockEntity.RATE),
                "Ranked adapter convert rate must match scalePulse of 20");
        h.assertTrue(adapter.pulseRate()>PulseAdapterBlockEntity.RATE,"Rank 1 must convert more than 20 Pulse/s");
        h.assertTrue(adapter.handler.receiveEnergy(1000,false)==0,"FE must not convert back to Pulse");
        h.runAfterDelay(45,()->{
            h.assertTrue(adapter.handler.getEnergyStored()>0,"Adapter must convert nearby Pulse");
            h.setBlock(2,2,3,Blocks.REDSTONE_BLOCK);
            h.assertTrue(adapter.handler.extractEnergy(1000,false)==0,"Powered adapter must block external FE extraction");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void playerMachinesDropWithoutATaggedTool(GameTestHelper h) {
        var ores=new java.util.HashSet<>(java.util.Set.of("march_stone","march_cobble","march_ore",
                "moonstone","moss_agate"));   // the Glimmer Ridge is world stone and mines like it
        tk.darrow.tribalpower.world.MarchOres.BLOCKS.keySet().forEach(mineral->ores.add("march_"+mineral+"_ore"));
        // The stone half of the March building set mines like vanilla stone.
        tk.darrow.tribalpower.world.MarchBuilding.ITEMS.keySet().stream()
                .filter(id->id.contains("stone")||id.contains("cobble")||id.contains("agate")).forEach(ores::add);
        var never=java.util.Set.of("gate_portal","spirit_light","spirit_click");
        for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if(!id.getNamespace().equals("tribalpower")||never.contains(id.getPath())) continue;
            boolean requires=block.defaultBlockState().requiresCorrectToolForDrops();
            if(ores.contains(id.getPath())) h.assertTrue(requires,id+" world stone still needs a pickaxe");
            else h.assertFalse(requires,id+" vanished because it required a tagged tool");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void echoStationDropsItselfAndContents(GameTestHelper h) {
        var pos=new BlockPos(2,1,2);
        h.setBlock(pos,ModBlocks.ECHO_SHATTER.get());
        var station=at(h,pos,EchoStationBlockEntity.class);
        station.setItem(0,new ItemStack(Items.STONE,8));
        station.setItem(1,new ItemStack(ModItems.ECHO_SHARD.get(),3));
        var abs=h.absolutePos(pos);
        var player=VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild=false;
        net.minecraft.world.level.block.Block.dropResources(h.getBlockState(pos),h.getLevel(),abs,station,player,ItemStack.EMPTY);
        h.setBlock(pos,Blocks.AIR);
        var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,h.getBounds().inflate(4));
        int machines=0,stone=0,shards=0;
        for(var item:items) {
            var stack=item.getItem();
            if(stack.is(ModBlocks.ECHO_SHATTER.get().asItem())) machines+=stack.getCount();
            if(stack.is(Items.STONE)) stone+=stack.getCount();
            if(stack.is(ModItems.ECHO_SHARD.get())) shards+=stack.getCount();
        }
        h.assertTrue(machines>=1,"Echo Shatter must drop itself, found "+machines);
        h.assertTrue(stone==8 && shards==3,"Station contents must drop, stone="+stone+" shards="+shards);
        h.succeed();
    }
    @GameTest(template="empty")
    public static void waveDrumKeepsItsTankWhenBroken(GameTestHelper h) {
        var pos=new BlockPos(2,1,2);
        h.setBlock(pos,tk.darrow.tribalpower.generator.GeneratorRegistry.WAVE_DRUM.get());
        var drum=at(h,pos,tk.darrow.tribalpower.generator.WaveDrumBlockEntity.class);
        drum.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var abs=h.absolutePos(pos);
        var player=VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild=false;
        net.minecraft.world.level.block.Block.dropResources(h.getBlockState(pos),h.getLevel(),abs,drum,player,ItemStack.EMPTY);
        h.setBlock(pos,Blocks.AIR);
        var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,h.getBounds().inflate(4));
        boolean kept=false;
        for(var item:items) {
            var stack=item.getItem();
            if(!stack.is(tk.darrow.tribalpower.generator.GeneratorRegistry.WAVE_DRUM.get().asItem())) continue;
            var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            h.assertTrue(data!=null,"Wave Drum must keep its block entity on the drop");
            kept=data.copyTag().contains("Tank");
        }
        h.assertTrue(kept,"Wave Drum water must ride on the dropped drum, not void");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void ritualChalkHasTenUsesPerStick(GameTestHelper h) {
        var stack=new ItemStack(ModItems.RITUAL_CHALK.get(),4);
        h.assertTrue(tk.darrow.tribalpower.item.RitualChalkItem.remaining(stack)==tk.darrow.tribalpower.item.RitualChalkItem.USES,"Each chalk stick starts at 10 uses");
        h.assertTrue(stack.getMaxStackSize()>=4,"A craft of four sticks must be able to stack");
        h.setBlock(2,1,2,Blocks.STONE);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var floor=h.absolutePos(new BlockPos(2,1,2));
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(floor.getCenter().add(0,0.5,0),Direction.UP,floor,false)));
        h.assertTrue(stack.getCount()==3,"Spending one mark splits one stick off a stack of four");
        h.assertTrue(tk.darrow.tribalpower.item.RitualChalkItem.remaining(stack)==tk.darrow.tribalpower.item.RitualChalkItem.USES,
                "Leftover sticks in the stack must still have ten uses");
        int used=0;
        for(var inv:player.getInventory().items) {
            if(inv.is(ModItems.RITUAL_CHALK.get()) && tk.darrow.tribalpower.item.RitualChalkItem.remaining(inv)<tk.darrow.tribalpower.item.RitualChalkItem.USES)
                used+=inv.getCount();
        }
        h.assertTrue(used==1 && tk.darrow.tribalpower.item.RitualChalkItem.remaining(findUsedChalk(player))==tk.darrow.tribalpower.item.RitualChalkItem.USES-1,
                "The spent stick must keep nine uses of its own");
        h.succeed();
    }
    private static ItemStack findUsedChalk(net.minecraft.world.entity.player.Player player) {
        for(var inv:player.getInventory().items) {
            if(inv.is(ModItems.RITUAL_CHALK.get()) && tk.darrow.tribalpower.item.RitualChalkItem.remaining(inv)<tk.darrow.tribalpower.item.RitualChalkItem.USES)
                return inv;
        }
        return ItemStack.EMPTY;
    }
    @GameTest(template="empty")
    public static void leyLensCyclesSightModes(GameTestHelper h) {
        var lens=new ItemStack(tk.darrow.tribalpower.ley.LeyRegistry.LEY_LENS.get());
        h.assertTrue(tk.darrow.tribalpower.ley.LeyLensItem.mode(lens)==tk.darrow.tribalpower.ley.LeyLensItem.LEY,"Fresh lens starts on ley sight");
        tk.darrow.tribalpower.ley.LeyLensItem.cycle(lens);
        h.assertTrue(tk.darrow.tribalpower.ley.LeyLensItem.mode(lens)==tk.darrow.tribalpower.ley.LeyLensItem.PULSE,"First use is pulse zone");
        tk.darrow.tribalpower.ley.LeyLensItem.cycle(lens);
        h.assertTrue(tk.darrow.tribalpower.ley.LeyLensItem.mode(lens)==tk.darrow.tribalpower.ley.LeyLensItem.VOICE,"Second use is voices");
        tk.darrow.tribalpower.ley.LeyLensItem.cycle(lens);
        h.assertTrue(tk.darrow.tribalpower.ley.LeyLensItem.mode(lens)==tk.darrow.tribalpower.ley.LeyLensItem.MACHINE,"Third use is machines");
        tk.darrow.tribalpower.ley.LeyLensItem.cycle(lens);
        h.assertTrue(tk.darrow.tribalpower.ley.LeyLensItem.mode(lens)==tk.darrow.tribalpower.ley.LeyLensItem.LEY,"Fourth use wraps to ley");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void stationAndCacheHonourSideIo(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.ECHO_SHATTER.get());
        var station=at(h,new BlockPos(2,2,2),EchoStationBlockEntity.class);
        h.assertTrue(station.sideIo().get(Direction.DOWN)==tk.darrow.tribalpower.lattice.SideIo.Mode.OUTPUT,"Stations default to output below");
        h.assertTrue(station.canTakeItemThroughFace(1,ItemStack.EMPTY,Direction.DOWN),"Output face may drain products");
        h.assertFalse(station.canPlaceItemThroughFace(0,new ItemStack(Items.STONE),Direction.DOWN),"Output face must refuse input");
        station.sideIo().set(Direction.UP,tk.darrow.tribalpower.lattice.SideIo.Mode.NONE);
        h.assertTrue(station.getSlotsForFace(Direction.UP).length==0,"Closed face exposes no slots");
        h.setBlock(4,2,2,ModBlocks.ANCESTRAL_CACHE.get());
        var cache=at(h,new BlockPos(4,2,2),AncestralCacheBlockEntity.class);
        cache.sideIo().set(Direction.UP,tk.darrow.tribalpower.lattice.SideIo.Mode.OUTPUT);
        var handler=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,h.absolutePos(new BlockPos(4,2,2)),Direction.UP);
        h.assertTrue(handler.insertItem(0,new ItemStack(Items.DIRT),false).getCount()==1,"Output-only cache face must refuse inserts");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void adjacentStationsShareConfiguredFaces(GameTestHelper h) {
        h.setBlock(2,3,2,ModBlocks.ECHO_SHATTER.get());
        h.setBlock(2,2,2,ModBlocks.EMBER_KILN.get());
        var shatter=at(h,new BlockPos(2,3,2),EchoStationBlockEntity.class);
        var kiln=at(h,new BlockPos(2,2,2),EchoStationBlockEntity.class);
        shatter.setItem(1,new ItemStack(ModItems.IRON_GRIT.get(),8));
        tk.darrow.tribalpower.lattice.SideIoAdjacency.push(h.getLevel(),shatter);
        h.assertTrue(shatter.getItem(1).isEmpty() && kiln.getItem(0).getCount()==8,
                "Shatter output-down into kiln input-up must move grit without a relay");
        h.setBlock(4,2,2,ModBlocks.ANCESTRAL_CACHE.get());
        h.setBlock(5,2,2,ModBlocks.ANCESTRAL_CACHE.get());
        var left=at(h,new BlockPos(4,2,2),AncestralCacheBlockEntity.class);
        var right=at(h,new BlockPos(5,2,2),AncestralCacheBlockEntity.class);
        left.setItem(0,new ItemStack(Items.DIAMOND,16));
        tk.darrow.tribalpower.lattice.SideIoAdjacency.push(h.getLevel(),left);
        h.assertTrue(left.countItem(Items.DIAMOND)==16 && right.isEmpty(),"Both-to-both faces must not ping-pong");
        left.sideIo().set(Direction.EAST,tk.darrow.tribalpower.lattice.SideIo.Mode.OUTPUT);
        right.sideIo().set(Direction.WEST,tk.darrow.tribalpower.lattice.SideIo.Mode.INPUT);
        tk.darrow.tribalpower.lattice.SideIoAdjacency.push(h.getLevel(),left);
        h.assertTrue(left.isEmpty() && right.countItem(Items.DIAMOND)==16,"Configured output-into-input must move the stack");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void relayRuneChoosesItemOrFluid(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.ITEM_RELAY.get());
        var relay=at(h,new BlockPos(2,2,2),WirelessRelayBlockEntity.class);
        h.assertFalse(relay.fluid(),"Item plate defaults to items");
        relay.setItem(WirelessRelayBlockEntity.RUNE,new ItemStack(ModItems.WATER_SEAL.get()));
        h.assertTrue(relay.fluid(),"Water Seal turns the plate to fluid");
        relay.setItem(WirelessRelayBlockEntity.RUNE,new ItemStack(ModItems.EARTH_SEAL.get()));
        h.assertFalse(relay.fluid(),"Earth Seal turns the plate to items");
        h.setBlock(4,2,2,ModBlocks.FLUID_RELAY.get());
        h.assertTrue(at(h,new BlockPos(4,2,2),WirelessRelayBlockEntity.class).fluid(),"Fluid plate defaults to fluid");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=120)
    public static void pairedRelaysShareABondItem(GameTestHelper h) {
        h.setBlock(2,2,2,ModBlocks.ITEM_RELAY.get());
        h.setBlock(2,1,2,Blocks.CHEST);
        h.setBlock(6,2,2,ModBlocks.ITEM_RELAY.get());
        h.setBlock(6,1,2,Blocks.CHEST);
        power(h);
        var source=at(h,new BlockPos(2,1,2),ChestBlockEntity.class);
        var dest=at(h,new BlockPos(6,1,2),ChestBlockEntity.class);
        source.setItem(0,new ItemStack(Items.GOLD_INGOT,16));
        var send=at(h,new BlockPos(2,2,2),WirelessRelayBlockEntity.class);
        var recv=at(h,new BlockPos(6,2,2),WirelessRelayBlockEntity.class);
        send.setItem(WirelessRelayBlockEntity.LINK,new ItemStack(Items.DIAMOND));
        recv.setItem(WirelessRelayBlockEntity.LINK,new ItemStack(Items.DIAMOND));
        send.extract(true);
        recv.extract(false);
        h.runAfterDelay(65,()->{
            h.assertTrue(source.isEmpty() && dest.countItem(Items.GOLD_INGOT)==16,"Paired plates must move the host chest into its partner");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void relayDropsBondAndKeepsTheTunerMark(GameTestHelper h) {
        h.setBlock(2,1,2,ModBlocks.ITEM_RELAY.get());
        h.setBlock(6,2,2,Blocks.CHEST);
        var relay=at(h,new BlockPos(2,1,2),WirelessRelayBlockEntity.class);
        relay.setItem(WirelessRelayBlockEntity.LINK,new ItemStack(Items.DIAMOND));
        relay.setItem(WirelessRelayBlockEntity.RUNE,new ItemStack(ModItems.EARTH_SEAL.get()));
        var dest=h.absolutePos(new BlockPos(6,2,2));
        h.assertTrue(relay.bind(dest,Direction.UP,h.getLevel().dimension().location().toString()),"The plate accepts a tuner mark");
        var pos=new BlockPos(2,1,2);
        var abs=h.absolutePos(pos);
        var player=VerificationPlayers.inLevel(h);
        player.getAbilities().instabuild=false;
        net.minecraft.world.level.block.Block.dropResources(h.getBlockState(pos),h.getLevel(),abs,relay,player,ItemStack.EMPTY);
        h.setBlock(pos,Blocks.AIR);
        var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,h.getBounds().inflate(4));
        int plates=0,diamonds=0,seals=0;
        boolean kept=false;
        for(var item:items) {
            var stack=item.getItem();
            if(stack.is(ModBlocks.ITEM_RELAY.get().asItem())) {
                plates+=stack.getCount();
                var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
                if(data!=null && data.copyTag().contains("Target") && data.copyTag().getLong("Target")==dest.asLong()) kept=true;
            }
            if(stack.is(Items.DIAMOND)) diamonds+=stack.getCount();
            if(stack.is(ModItems.EARTH_SEAL.get())) seals+=stack.getCount();
        }
        h.assertTrue(plates>=1,"The plate must drop itself");
        h.assertTrue(diamonds==1 && seals==1,"Bond and Rune must dump beside the plate, diamonds="+diamonds+" seals="+seals);
        h.assertTrue(kept,"A tuner-bound destination survives on the dropped plate");
        h.succeed();
    }
}
