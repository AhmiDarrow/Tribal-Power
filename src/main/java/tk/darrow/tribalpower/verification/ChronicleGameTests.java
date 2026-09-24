package tk.darrow.tribalpower.verification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.lore.CarvedStoneBlock;
import tk.darrow.tribalpower.lore.Chronicle;
import tk.darrow.tribalpower.lore.LoreRegistry;
import tk.darrow.tribalpower.lore.MuralBlock;
import tk.darrow.tribalpower.quest.QuestEvents;
import tk.darrow.tribalpower.quest.QuestSavedData;
import tk.darrow.tribalpower.tribe.CodexUnlocksPayload;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;
import tk.darrow.tribalpower.tribe.TribeStanding;

/** The world's own story: every fragment stands somewhere, reading is kept, murals hang whole, paintings and crests exist. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class ChronicleGameTests {
    private ChronicleGameTests() {}

    private static final List<String> CARRIERS = List.of("loom_ruin", "stilt_village_ruin", "sunken_shrine", "bog_barrow", "ash_tomb", "ossuary",
            "delving_gallery", "glimmer_vault", "slag_throne", "drowned_root", "cairn_ring", "singing_fracture", "sealed_gate", "tide_stone", "trampled_ring", "roost");

    @GameTest(template = "empty")
    public static void everyChronicleFragmentStandsSomewhere(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        Set<Integer> found = new HashSet<>();
        for (String name : CARRIERS) {
            var template = manager.get(ResourceLocation.fromNamespaceAndPath("tribalpower", name));
            h.assertTrue(template.isPresent(), name + " exists");
            for (var block : List.of(LoreRegistry.CARVED_STONE.get(), LoreRegistry.MURAL.get()))
                for (var info : template.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block))
                    found.add(info.state().getValue(CarvedStoneBlock.FRAGMENT));
        }
        for (int i = 0; i < Chronicle.FRAGMENTS; i++) h.assertTrue(found.contains(i), "Fragment " + i + " is carved somewhere in the March");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void readingIsKeptAndTheCodexHearsOfIt(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            h.assertTrue(Chronicle.readCount(player) == 0, "Nothing read yet");
            h.assertTrue(Chronicle.markRead(player, 3), "First read counts");
            h.assertFalse(Chronicle.markRead(player, 3), "A second read does not");
            h.assertTrue(Chronicle.hasRead(player, 3) && Chronicle.readCount(player) == 1, "The fragment is kept");
            h.assertTrue(CodexUnlocksPayload.of(player).fragments() == (1 << 3), "The payload carries it");
            for (int i = 0; i < Chronicle.FRAGMENTS; i++) Chronicle.markRead(player, i);
            h.assertTrue(player.getAdvancements().getOrStartProgress(player.server.getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath("tribalpower", "march/chronicle"))).isDone(), "All sixteen is the advancement");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aMuralHangsInFourPartsAndComesDownTogether(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            BlockPos wall = new BlockPos(3, 2, 6);
            for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++) h.setBlock(wall.offset(dx, dy, 0), Blocks.STONE);
            BlockPos origin = wall.offset(0, 0, -1);
            var state = LoreRegistry.MURAL.get().defaultBlockState().setValue(MuralBlock.FACING, Direction.NORTH).setValue(MuralBlock.FRAGMENT, 9);
            h.setBlock(origin, state);
            LoreRegistry.MURAL.get().setPlacedBy(h.getLevel(), h.absolutePos(origin), state, player, net.minecraft.world.item.ItemStack.EMPTY);
            for (int part = 0; part < 4; part++) {
                BlockPos at = MuralBlock.partPos(h.absolutePos(origin), Direction.NORTH, part);
                var there = h.getLevel().getBlockState(at);
                h.assertTrue(there.is(LoreRegistry.MURAL.get()) && there.getValue(MuralBlock.PART) == part && there.getValue(MuralBlock.FRAGMENT) == 9, "Part " + part + " hangs");
                h.assertTrue(MuralBlock.origin(at, there).equals(h.absolutePos(origin)), "Part " + part + " knows its origin");
            }
            BlockPos top = MuralBlock.partPos(h.absolutePos(origin), Direction.NORTH, 3);
            LoreRegistry.MURAL.get().playerWillDestroy(h.getLevel(), top, h.getLevel().getBlockState(top), player);
            h.getLevel().removeBlock(top, false);
            for (int part = 0; part < 4; part++)
                h.assertFalse(h.getLevel().getBlockState(MuralBlock.partPos(h.absolutePos(origin), Direction.NORTH, part)).is(LoreRegistry.MURAL.get()), "Part " + part + " comes down with the rest");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void paintingsAndCrestsAreRegisteredAndKinGetTheirCrest(GameTestHelper h) {
        var paintings = h.getLevel().registryAccess().registryOrThrow(Registries.PAINTING_VARIANT);
        var placeable = TagKey.create(Registries.PAINTING_VARIANT, ResourceLocation.withDefaultNamespace("placeable"));
        for (String id : List.of("aurora_over_the_march", "the_drum_circle", "nine_hearths", "the_loom", "the_edge", "the_singing_fracture")) {
            var holder = paintings.getHolder(ResourceLocation.fromNamespaceAndPath("tribalpower", id));
            h.assertTrue(holder.isPresent(), "Painting " + id + " exists");
            h.assertTrue(holder.get().is(placeable), "Painting " + id + " can be hung");
            PaintingVariant variant = holder.get().value();
            h.assertTrue(variant.width() >= 1 && variant.height() >= 1, id + " has a size");
        }
        var patterns = h.getLevel().registryAccess().registryOrThrow(Registries.BANNER_PATTERN);
        for (TribeDefinition tribe : TribeDefinition.values()) {
            var holder = patterns.getHolder(ResourceLocation.fromNamespaceAndPath("tribalpower", tribe.id() + "_crest"));
            h.assertTrue(holder.isPresent(), tribe.id() + " crest exists");
            h.assertTrue(holder.get().is(LoreRegistry.PATTERN_TAGS.get(tribe)), tribe.id() + " crest is in its pattern item's tag");
        }
        var player = VerificationPlayers.inLevel(h);
        try {
            TribeDefinition tribe = TribeDefinition.SWARM;
            TribeStanding.add(player, tribe, TribeRank.KIN.threshold());
            QuestEvents.standingChanged(player, tribe);
            h.assertTrue(QuestSavedData.get(player.server).hasPattern(player.getUUID(), tribe), "Kin are given the crest");
            h.assertTrue(player.getInventory().countItem(LoreRegistry.PATTERN_ITEMS.get(tribe).get()) == 1, "The pattern item lands in the pack");
            QuestEvents.standingChanged(player, tribe);
            h.assertTrue(player.getInventory().countItem(LoreRegistry.PATTERN_ITEMS.get(tribe).get()) == 1, "Only once");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }
}
