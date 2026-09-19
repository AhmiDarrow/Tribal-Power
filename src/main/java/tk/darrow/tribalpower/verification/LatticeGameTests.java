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

    @GameTest(template="empty")
    public static void codexHasValidItemsAndSpoilerSafeLanding(GameTestHelper h) {
        var entries=tk.darrow.tribalpower.guide.CodexEntries.ALL;
        h.assertTrue(entries.size()>=54 && !entries.getFirst().spoiler(),"Complete Codex needs a spoiler-safe landing");
        h.assertTrue(entries.stream().anyMatch(e->"chapter_1".equals(e.id()) && e.text().contains("JEI")),"The landing page must mention JEI as an optional recipe link");
        h.assertTrue(entries.stream().anyMatch(e->"walk_pulse_resonator".equals(e.id()) && e.text().contains("Echo Shard")),"Codex must teach seating a Resonator");
        h.assertTrue(entries.stream().anyMatch(e->"walk_lattice_conductor".equals(e.id()) && e.text().contains("Ritual Chalk") && e.text().contains("voice-craft")),"A Conductor walkthrough must name voice-crafts, not only the three starter drums");
        h.assertTrue(entries.stream().anyMatch(e->"walk_offering_table".equals(e.id()) && e.text().contains("27") && !e.text().contains("Standing still applies")),"Offering Table is storage, not a hearth");
        h.assertTrue(entries.stream().anyMatch(e->"walk_spring_calling".equals(e.id()) && e.text().contains("Rite Pedestals")),"Spring Calling is a tablet rite and needs its circle");
        h.assertTrue(entries.stream().anyMatch(e->"walk_pulse_threshold".equals(e.id()) && e.text().contains("Inverse") && e.text().contains("at or above")),"A Threshold sings while Pulse is high");
        h.assertTrue(entries.stream().anyMatch(e->"walk_rain_chime".equals(e.id()) && e.text().contains("Comparator: 8 for rain")),"Rain Chime is a weather comparator, not an open-sky generator");
        h.assertTrue(entries.stream().anyMatch(e->"walk_gate_drum".equals(e.id()) && e.text().contains("Travel costs 20") && e.text().contains("25 Pulse") && !e.text().contains("Place and strike")),"The Gate Drum spends stored Pulse; a cell adds 25 per use");
        h.assertTrue(entries.stream().anyMatch(e->"walk_spring_calling".equals(e.id()) && !e.text().contains("while you stand still")),"A spring fills the cistern without the player standing there");
        h.assertTrue(entries.stream().anyMatch(e->"walk_ley_collector".equals(e.id()) && e.text().contains("You do not have to stand there") && !e.text().contains("base trickle")),"A Ley Collector generates from landscape; a roof is weaker, not dead");
        h.assertTrue(entries.stream().anyMatch(e->"walk_binding_effigy".equals(e.id()) && e.text().contains("80 Pulse")),"A Summoning Cradle spends 80 Pulse per summon");
        h.assertTrue(entries.stream().anyMatch(e->"walk_voice_ring".equals(e.id()) && e.text().contains("Only Answered Resonance Totems count")),"The Voice Ring still requires Answered totems");
        h.assertTrue(entries.stream().anyMatch(e->"walk_kinship_totem".equals(e.id()) && e.text().contains("caps at five")),"Kinship fifteen-voice totals need a Voice Ring");
        h.assertTrue(entries.stream().anyMatch(e->"chapter_26".equals(e.id()) && e.text().contains("spends 20 stored Pulse") && e.text().contains("refunds that 20") && !e.text().contains("Place and strike")),"The March page must not teach striking the Gate Drum; a blocked landing refunds travel");
        h.assertTrue(entries.stream().anyMatch(e->"Machine ranks".equals(e.title()) && e.text().contains("Attune 16s/48 Pulse a second")),"Machine rank formulae are twice Spiritgear: Attune 16s/48 Pulse a second");
        h.assertTrue(entries.stream().anyMatch(e->"walk_waystone".equals(e.id()) && e.text().contains("spends nothing") && !e.text().contains("refunds Pulse")),"A compass checks the landing before it spends Pulse");
        h.assertTrue(entries.stream().anyMatch(e->"Waystone paths".equals(e.title()) && e.text().contains("spends nothing")),"Waystone paths must not teach a blocked compass as a refund");
        h.assertTrue(entries.stream().anyMatch(e->"rite_spring_calling".equals(e.id()) && e.text().contains("Rite Pedestals")),"Spring Calling teaching must require the circle");
        h.assertTrue(entries.stream().anyMatch(e->"pulse_logic".equals(e.id()) && e.text().contains("Inverse") && e.text().contains("at or above")),"Pulse logic must teach Threshold polarity");
        h.assertTrue(entries.stream().anyMatch(e->"walk_loom_anchor".equals(e.id()) && e.text().contains("Quiet totems still count")),"Loom Anchor counts nearby voices even when they are quiet");
        h.assertTrue(entries.stream().anyMatch(e->"walk_stone_font".equals(e.id()) && e.text().contains("output only") && !e.text().contains("Cobble in, stone out")),"The Stone Font produces cobble; it does not take cobble as input");
        h.assertTrue(entries.stream().anyMatch(e->"gate_far".equals(e.id()) && e.text().contains("1,200 Pulse")),"Far Gate lighting is 1200 of 2000, not the Way Gate 400");
        h.assertTrue(entries.stream().anyMatch(e->"voice_loom_anchor".equals(e.id()) && e.text().contains("Quiet totems still count")),"Loom Anchor page must count quiet totems");
        h.assertTrue(entries.stream().anyMatch(e->"walk_verse_wireless".equals(e.id()) && e.text().contains("copies that strength")),"A Verse Answer copies the Call; it is not always 15");
        h.assertTrue(entries.stream().anyMatch(e->"ember_kiln".equals(e.id()) && e.text().contains("Cobble becomes stone") && e.text().contains("32 Pulse a second")),"Ember Kiln cobble is a furnace smelt; Pulse is per second, not per craft");
        h.assertTrue(entries.stream().anyMatch(e->"walk_ember_kiln".equals(e.id()) && e.text().contains("Cobble becomes stone") && e.text().contains("32 Pulse a second") && !e.text().contains("Grit or cobble goes in")),"Kiln walkthrough must not treat cobble as an ingot path; Pulse is per second");
        h.assertTrue(entries.stream().anyMatch(e->"pit_bands".equals(e.id()) && e.text().contains("28 Pulse") && !e.text().contains("14 Pulse") && !e.text().contains("lapis and quartz")),"Pit bands must match OreBand: 28/32 Pulse, quartz is hot only");
        h.assertTrue(entries.stream().anyMatch(e->"pit_sample".equals(e.id()) && e.text().contains("280 Pulse") && !e.text().contains("70 Pulse")),"Iron from the pit spends 280 Pulse a cycle, not the old 70");
        h.assertTrue(entries.stream().anyMatch(e->"voice_pulse_cairn".equals(e.id()) && e.text().contains("200 Pulse a second")),"A cairn swallows 200 Pulse a second");
        h.assertTrue(entries.stream().anyMatch(e->"spirit_charms".equals(e.id()) && e.text().contains("silent Chorus still costs 2")),"An unbound Chorus still spends Pulse");
        h.assertTrue(entries.stream().anyMatch(e->"gate_way".equals(e.id()) && e.text().contains("one-second cooldown")),"Way Gates apply a one-second bounce-back cooldown");
        h.assertTrue(entries.stream().anyMatch(e->"camp_grove_tender".equals(e.id()) && e.text().contains("Place it yourself")),"A Grove Tender must be placed by a player to claim it");
        h.assertTrue(entries.stream().anyMatch(e->"walk_listening_pit".equals(e.id()) && e.text().contains("Quartz needs Fire") && e.text().contains("Friend standing")),"The pit walkthrough must not send quartz to the deep band; Friend standing is required even for common");
        h.assertTrue(entries.stream().anyMatch(e->"Deep Listening".equals(e.title()) && e.text().contains("Kin for deep and hot") && e.text().contains("Deep, hot and rare all need it") && !e.text().contains("Voice and their kinship for rare")),"Deep Listening kinship is required for deep, hot and rare, not only rare");
        h.assertTrue(entries.stream().anyMatch(e->"walk_kinship_totem".equals(e.id()) && e.text().contains("not only rare")),"Kinship teaching must not claim Deep Listening kinship is rare-only");
        h.assertTrue(entries.stream().anyMatch(e->"Pulse to FE".equals(e.title()) && e.text().contains("cannot receive FE")),"The Pulse Adapter exports FE and cannot receive it");
        h.assertTrue(entries.stream().anyMatch(e->"Walkthrough: convert Pulse to FE".equals(e.title()) && e.text().contains("cannot receive FE")),"The adapter walkthrough must say it cannot receive FE");
        h.assertTrue(entries.stream().anyMatch(e->"Song Bench".equals(e.title()) && e.text().contains("8 Pulse a tick") && e.text().contains("cost much less")),"The Song Bench is hungrier than dedicated Echo stations");
        h.assertTrue(entries.stream().anyMatch(e->"Spirit Charms".equals(e.title()) && e.text().contains("40 Pulse each")),"Adding a charm voice costs 40 Pulse");
        h.assertTrue(entries.stream().anyMatch(e->"Keeping".equals(e.title()) && e.text().contains("40 minutes of loaded time") && e.text().contains("30% longer")),"Keeping dim stretches station time about 30%");
        h.assertTrue(entries.stream().anyMatch(e->"Tribal Kin".equals(e.title()) && e.text().contains("once per Minecraft day")),"Elders restock once per Minecraft day");
        h.assertTrue(entries.stream().anyMatch(e->"Machine ranks".equals(e.title()) && e.text().contains("15% Pulse a second per rank") && !e.text().contains("Ranked generators hum harder")),"Ranked generators add 15% per rank, not workshop 118%");
        h.assertTrue(entries.stream().anyMatch(e->"Drumheart".equals(e.title()) && e.text().contains("holds 1,000 Pulse") && e.text().contains("15% Pulse a beat per rank")),"A Drumheart holds 1,000 Pulse and ranks at 15% per beat");
        h.assertTrue(entries.stream().anyMatch(e->"Totem-bound gear".equals(e.title()) && e.text().contains("one projectile in five")),"An Earth hood turns aside one projectile in five");
        h.assertTrue(entries.stream().anyMatch(e->"Sixfold Staff".equals(e.title()) && e.text().contains("within 12 blocks")),"Spirit staff reveal reaches 12 blocks");
        h.assertTrue(entries.stream().anyMatch(e->"Ley Collector".equals(e.title()) && e.text().contains("holds 2,000 Pulse") && e.text().contains("15% Pulse a second per rank") && !e.text().contains("hum harder")),"A Ley Collector holds 2,000 Pulse and ranks at 15% per rank");
        h.assertTrue(entries.stream().anyMatch(e->"Pulse Resonator".equals(e.title()) && e.text().contains("holds 2,500 Pulse")),"A Pulse Resonator holds 2,500 Pulse");
        h.assertTrue(entries.stream().anyMatch(e->"Spirit Pulse".equals(e.title()) && e.text().contains("Each totem holds 250 Pulse") && e.text().contains("tools, spells and travel") && !e.text().contains("tools and other uses")),"A Pulse Cell carries Pulse for tools, spells and travel");
        h.assertTrue(entries.stream().anyMatch(e->"Chalk and conductor".equals(e.title()) && e.text().contains("voice-craft") && e.text().contains("nearby generators") && e.text().contains("Each totem holds 250 Pulse")),"A Conductor drains any nearby generator, not only the three starter drums");
        h.assertTrue(entries.stream().anyMatch(e->"Resonance Maul".equals(e.title()) && e.text().contains("rests one second")),"The Resonance Maul rests one second after a break");
        h.assertTrue(entries.stream().anyMatch(e->"Spirit Charms".equals(e.title()) && e.text().contains("one projectile in four")),"Ward turns aside one projectile in four");
        h.assertTrue(entries.stream().anyMatch(e->"Spiritgear ranks".equals(e.title()) && e.text().contains("mining costs 2") && e.text().contains("a hit costs 3")),"Spiritgear action Pulse is 2 to mine and 3 to hit");
        h.assertTrue(entries.stream().anyMatch(e->"Loom Anchor".equals(e.title()) && e.text().contains("15% Pulse a second per rank") && !e.text().contains("hum harder")),"A ranked Loom Anchor adds 15% Pulse a second per rank");
        h.assertTrue(entries.stream().anyMatch(e->"What the ground offers".equals(e.title()) && e.text().contains("before pack settings") && e.text().contains("28 Pulse")),"Pit Pulse is the OreBand table before the consumption multiplier");
        h.assertTrue(entries.stream().anyMatch(e->"Sample and substrate".equals(e.title()) && e.text().contains("280 Pulse") && e.text().contains("before pack settings") && e.text().contains("560 at the default")),"One iron cycle is 280 before pack settings, 560 at default 2.0");
        h.assertTrue(entries.stream().anyMatch(e->"Listening Pit".equals(e.title()) && e.text().contains("200 Pulse buffer")),"The Resonance Mesh holds a 200 Pulse buffer");
        h.assertTrue(entries.stream().anyMatch(e->"Ley Lens".equals(e.title()) && e.text().contains("3 at night") && e.text().contains("Cap 16")),"Ley Lens must name LeyMath factor amounts");
        h.assertTrue(entries.stream().anyMatch(e->"Totem-bound gear".equals(e.title()) && e.text().contains("Earth hood")),"Totem-bound armor perks must be named, not only pick perks");
        h.assertTrue(entries.stream().anyMatch(e->"Cargo tiers".equals(e.title()) && e.text().contains("before pack settings") && e.text().contains("Cost 4 Pulse")),"Relay Pulse is the table before the consumption multiplier");
        h.assertTrue(entries.stream().anyMatch(e->"Stone Font".equals(e.title()) && e.text().contains("do not touch cobble") && e.text().contains("before pack settings")),"Stone Font cobble is exempt from pack consumption; stone and obsidian are not");
        h.assertTrue(entries.stream().anyMatch(e->"Shatter".equals(e.title()) && e.text().contains("20 Pulse a second before pack settings")),"Grit shatter is 4s/20 Pulse before pack settings");
        h.assertTrue(entries.stream().anyMatch(e->"walk_waystone".equals(e.id()) && e.text().contains("Fill a Pulse Cell") && !e.text().contains("Charge a Pulse Cell")),"A blocked compass trip does not ask you to charge a cell as if the landing spent Pulse");
        h.assertTrue(entries.stream().anyMatch(e->"Sixfold Staff".equals(e.title()) && e.text().contains("up to 8 blocks")),"Loom tether pulls up to 8 blocks");
        h.assertTrue(entries.stream().anyMatch(e->"Spiritgear ranks".equals(e.title()) && e.text().contains("8 seconds and 24 Pulse a second")),"Spiritgear Attune is 8s/24 Pulse a second, half the machine formula");
        h.assertTrue(entries.stream().anyMatch(e->"Tribal Kin".equals(e.title()) && e.text().contains("every 6 seconds") && e.text().contains("Drumheart, Ley Collector or Pulse Resonator") && !e.text().contains("generators within 8 blocks")),"A Drummer feeds Drumheart, Ley Collector and Pulse Resonator only");
        h.assertTrue(entries.stream().anyMatch(e->"Standing".equals(e.title()) && e.text().contains("60 standing") && e.text().contains("at most 40 Pulse") && e.text().contains("breaking a banner costs 5") && !e.text().contains("camp blocks")),"Hearth offerings cap at 60 standing a day; a banner costs 5, not generic camp blocks");
        h.assertTrue(entries.stream().anyMatch(e->"Redstone language".equals(e.title()) && e.text().contains("other than the Drumheart") && !e.text().contains("and every generator")),"Held redstone pauses generators other than the Drumheart");
        h.assertTrue(entries.stream().anyMatch(e->"Pulse lights".equals(e.title()) && e.text().contains("1 Pulse a second") && e.text().contains("Glow Reed") && e.text().contains("Wind Charm")),"Pulse lights draw 1 or 2 Pulse a second; a Wind Charm spends none");
        h.assertTrue(entries.stream().anyMatch(e->"Wind Charm".equals(e.title()) && e.text().contains("no collision") && e.text().contains("spends no Pulse")),"A Wind Charm hangs from a ceiling, has no collision, and spends no Pulse");
        h.assertTrue(entries.stream().anyMatch(e->"Ember Horn".equals(e.title()) && e.text().contains("15% Pulse a second per rank") && !e.text().contains("the rate is the real cap")),"An Ember Horn's 20 a second is the unranked cap; rank adds 15%");
        h.assertTrue(entries.stream().anyMatch(e->"Camp blessings".equals(e.title()) && e.text().contains("Loom grants luck") && e.text().contains("threads 2 Pulse")),"Camp blessings must include the Loom luck and cell-tension beat");
        h.assertTrue(entries.stream().anyMatch(e->"Ley Lens".equals(e.title()) && e.text().contains("weaker, not dead") && !e.text().contains("starves a collector")),"Ley Lens must not teach that a roof starves a collector");
        h.assertTrue(entries.stream().anyMatch(e->"Ley Collector".equals(e.title()) && e.text().contains("a hearth and animals still add") && e.text().contains("every two seconds") && e.text().contains("that beat, not Pulse a second") && e.text().contains("8 of 16") && !e.text().contains("half the cap")),"A Ley Collector pads totems from landscape strength (8 of 16), not stored Pulse");
        h.assertTrue(entries.stream().anyMatch(e->"walk_ley_collector".equals(e.id()) && e.text().contains("every two seconds")),"Ley Collector walkthrough must name the two-second beat");
        h.assertTrue(entries.stream().anyMatch(e->"Pulse Resonator".equals(e.title()) && e.text().contains("kinship included")),"A Resonator heap caps at five voices including kinship");
        h.assertTrue(entries.stream().anyMatch(e->"Walkthrough: your first rite".equals(e.title()) && e.text().contains("A cell in your pocket is not in range") && !e.text().contains("Greater Pulse Cell in a Drumheart")),"Tablet rites spend nearby generator Pulse, not a seated cell");
        h.assertTrue(entries.stream().anyMatch(e->"walk_stone_font".equals(e.id()) && e.text().contains("24 Pulse a second") && e.text().contains("obsidian at 6")),"Stone Font walkthrough must name both obsidian Pulse rates");
        h.assertTrue(entries.stream().anyMatch(e->"Carry the beat".equals(e.title()) && e.text().contains("fill the cell from that store") && e.text().contains("25 Pulse per use") && e.text().contains("voice-craft") && e.text().contains("Ley Collector does not fill") && !e.text().contains("spend it on tools, rites") && !e.text().contains("travel and tablet rites")),"A cell fills from a Drumheart (25), Resonator or voice-craft (100); a Ley Collector does not fill a cell");
        h.assertTrue(entries.stream().anyMatch(e->"Walkthrough: tend a grove".equals(e.title()) && e.text().contains("voice-craft") && !e.text().contains("Drumheart or Resonator within 8")),"A Grove Tender drinks from any nearby generator");
        h.assertTrue(entries.stream().anyMatch(e->"Staff costs".equals(e.title()) && e.text().contains("Stitch rests 1.5 seconds")),"Loom stitch cooldown is 30 ticks, not the two-second voice rest");
        h.assertTrue(entries.stream().anyMatch(e->"Walkthrough: cast the Sixfold Staff".equals(e.title()) && e.text().contains("Stitch rests 1.5 seconds")),"Staff walkthrough must name the stitch rest");
        h.assertTrue(entries.stream().anyMatch(e->"Spiritweave".equals(e.title()) && e.text().contains("Unlinked pieces spend 2 Pulse") && e.text().contains("3 Pulse upkeep")),"Bound Spiritweave spends 3 Pulse; unlinked spends 2");
        h.assertTrue(entries.stream().anyMatch(e->"Verse Call and Answer".equals(e.title()) && e.text().contains("copies that strength")),"A Verse Answer copies analog strength; it is not always 15");
        h.assertTrue(entries.stream().anyMatch(e->"walk_wind_harp".equals(e.id()) && e.text().contains("unranked cap") && e.text().contains("15% Pulse a second per rank") && !e.text().contains("the whole of the wind")),"A Wind Harp's six a second is the unranked cap; rank adds 15%");
        h.assertTrue(entries.stream().anyMatch(e->"walk_wake_bell".equals(e.id()) && e.text().contains("unranked") && e.text().contains("15% Pulse a second per rank")),"A Wake Bell's 8 a second is the unranked cap; rank adds 15%");
        h.assertTrue(entries.stream().anyMatch(e->"Loom Anchor".equals(e.title()) && e.text().contains("not a seventh voice") && !e.text().contains("extra tribe voices toward that count")),"A Loom Anchor kinship totem lends an element, not a seventh voice");
        h.assertTrue(entries.stream().anyMatch(e->"Combat and company".equals(e.title()) && e.text().contains("refunds 4 Pulse") && !e.text().contains("a little Pulse")),"A Cinder Imp refunds 4 Pulse on a hand-drum, Tempo +2");
        h.assertTrue(entries.stream().anyMatch(e->"Stone Font".equals(e.title()) && e.text().contains("pit-speed")),"Stone Font asks honour the same pit-speed setting as the pit");
        h.assertTrue(entries.stream().anyMatch(e->"Wave Drum".equals(e.title()) && e.text().contains("15% Pulse a second per rank")),"A ranked Wave Drum adds 15% Pulse a second per rank");
        h.assertTrue(entries.stream().anyMatch(e->"Six voices".equals(e.title()) && e.text().contains("lend that tribe's element")),"Kinship Totems lend an element to stations; extra tribe voices are Resonator-only");
        h.assertTrue(entries.stream().anyMatch(e->"Camp hands".equals(e.title()) && e.text().contains("voice-craft")),"Camp hands drink from any nearby generator, including voice-crafts");
        h.assertTrue(entries.stream().anyMatch(e->"Pulse Resonator".equals(e.title()) && e.text().contains("15% Pulse a second per rank")),"A ranked Resonator adds 15% Pulse a second per rank");
        h.assertTrue(entries.stream().anyMatch(e->"Kinship Totem".equals(e.title()) && e.text().contains("not a seventh voice")),"Kinship on a Loom or station is an element, not a seventh voice");
        h.assertTrue(entries.stream().anyMatch(e->"Hush Totem".equals(e.title()) && e.text().contains("2,400-Pulse reserve")),"Camp devices share a 2,400 Pulse reserve");
        h.assertTrue(entries.stream().anyMatch(e->"Hush Totem".equals(e.title()) && e.text().contains("refill up to 80 Pulse a second")),"Camp devices refill up to 80 Pulse a second from nearby generators");
        h.assertTrue(entries.stream().anyMatch(e->"Wayanchor".equals(e.title()) && e.text().contains("refill up to 80 Pulse a second")),"A Wayanchor refills up to 80 Pulse a second from nearby generators");
        h.assertTrue(entries.stream().anyMatch(e->"Sixfold Staff".equals(e.title()) && e.text().contains("ignites for 4 seconds") && e.text().contains("18 blocks") && !e.text().contains("nearby enemies")),"Staff Fire ignites 4 seconds; offensive spells reach 18 blocks");
        h.assertTrue(entries.stream().anyMatch(e->"Combat and company".equals(e.title()) && e.text().contains("within 4 blocks") && e.text().contains("reaches 8") && !e.text().contains("nearby drops")),"An Echo Weaver gathers within 4 blocks; Gather reaches 8");
        h.assertTrue(entries.stream().anyMatch(e->"Echo Unweave".equals(e.title()) && e.text().contains("2 Attuned Echoes") && e.text().contains("2 Echo Shards") && e.text().contains("2 wool")),"Unweave reverses Bound Echo, Attuned Echo and Spiritweave as well as Ingot and Core");
        var ids=new java.util.HashSet<String>();
        for(var entry:entries) {
            h.assertTrue(ids.add(entry.id()),"Duplicate Codex id: "+entry.id());
            var id=net.minecraft.resources.ResourceLocation.parse("tribalpower:"+entry.icon());
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id),"Unknown Codex item: "+id);
            h.assertTrue(!entry.text().isBlank(),"Empty teaching: "+entry.id());
            if(!entry.picture().isEmpty())h.assertTrue(entry.spoiler()||entry.safePicture(),"Pictures need spoiler protection unless the catalog marks them safe: "+entry.id());
            if(entry.safePicture())h.assertTrue(!entry.picture().isEmpty()&&!entry.spoiler(),"safe_picture is only for pictured, spoiler-free pages: "+entry.id());
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
    @GameTest(template="empty")
    public static void aGateDrumKeepsTravelPulseFromTheLattice(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,ModBlocks.GATE_DRUM.get());
        var drum=at(h,pos,GateDrumBlockEntity.class);
        drum.insertPulse(40,false);
        tk.darrow.tribalpower.lattice.LatticeNetwork.extractPulseNearby(h.getLevel(),h.absolutePos(pos),8,20,false);
        h.assertTrue(drum.getPulseStored()==40,"Nearby extract must not steal travel Pulse");
        h.assertTrue(drum.extractPulse(20,false)==0,"The lattice face of a Gate Drum is closed");
        h.assertTrue(drum.tryConsumeTravelPulse() && drum.getPulseStored()==20,"Travel still spends the drum");
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
        var ores=new java.util.HashSet<>(java.util.Set.of("march_stone","march_cobble","march_ore"));
        tk.darrow.tribalpower.world.MarchOres.BLOCKS.keySet().forEach(mineral->ores.add("march_"+mineral+"_ore"));
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
