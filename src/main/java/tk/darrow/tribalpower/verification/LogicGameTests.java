package tk.darrow.tribalpower.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.camp.identity.Camps;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.logic.LogicPlateBlock;
import tk.darrow.tribalpower.logic.LogicPlateBlockEntity;
import tk.darrow.tribalpower.logic.LogicPlateBlockEntity.Ear;
import tk.darrow.tribalpower.logic.LogicRegistry;
import tk.darrow.tribalpower.logic.PlateLinks;
import tk.darrow.tribalpower.logic.VerseLinkBlock;
import tk.darrow.tribalpower.logic.VerseLinkBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Song plates synced with the Totem Wrench: wireless, one-way, and safe in a ring. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class LogicGameTests {
    /** A floor plate (facing up) on a stone block. On the floor, left is west and right is east. */
    private static LogicPlateBlockEntity plate(GameTestHelper h, BlockPos pos, Block kind) {
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos, kind.defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.UP));
        return (LogicPlateBlockEntity) h.getBlockEntity(pos);
    }

    private static ServerPlayer wrencher(GameTestHelper h, ItemStack wrench) {
        ServerPlayer player = VerificationPlayers.inLevel(h);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        return player;
    }

    /** Uses the wrench on a plate; {@code west} is how far toward the plate's west (left) half the click lands. */
    private static InteractionResult wrench(GameTestHelper h, ServerPlayer player, ItemStack wrench, BlockPos pos, boolean crouch, double west) {
        player.setShiftKeyDown(crouch);
        BlockPos at = h.absolutePos(pos);
        Vec3 hit = Vec3.atCenterOf(at).add(-west, 0.3, 0);
        InteractionResult result = wrench.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, wrench,
                new BlockHitResult(hit, Direction.UP, at, false)));
        player.setShiftKeyDown(false);
        return result;
    }

    /** Crouch-uses a block with an empty hand, the way a plate's setting is changed. */
    private static void crouchUse(GameTestHelper h, ServerPlayer player, BlockPos pos) {
        BlockPos at = h.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        h.getBlockState(pos).useWithoutItem(h.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        player.setShiftKeyDown(false);
    }

    /** Sets a plate down the way a player's hand does, so the placer is recorded; null for a placer that is no player. */
    private static void placeBy(GameTestHelper h, BlockPos pos, net.minecraft.world.entity.LivingEntity placer) {
        h.getBlockState(pos).getBlock().setPlacedBy(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos), placer, ItemStack.EMPTY);
    }

    private static int threshold(GameTestHelper h, LogicPlateBlockEntity tally) {
        return tally.saveWithoutMetadata(h.getLevel().registryAccess()).getInt("Threshold");
    }

    private static void leave(GameTestHelper h, ServerPlayer... players) {
        for (ServerPlayer player : players) h.getLevel().getServer().getPlayerList().remove(player);
    }

    private static int power(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).getValue(LogicPlateBlock.POWER);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void wrenchSyncsAPlateWithNoWireBetween(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), listener = new BlockPos(6, 2, 6);
        plate(h, source, LogicRegistry.INVERSE_PLATE.get());   // nothing wired in, so it sings 15
        LogicPlateBlockEntity echo = plate(h, listener, LogicRegistry.ECHO_PLATE.get());
        ItemStack wrench = new ItemStack(ModItems.TOTEM_WRENCH.get());
        ServerPlayer player = wrencher(h, wrench);
        wrench(h, player, wrench, source, true, 0);
        GlobalPos held = PlateLinks.held(wrench);
        h.assertTrue(held != null && held.pos().equals(h.absolutePos(source)), "Crouch-use holds the source plate's song");
        wrench(h, player, wrench, listener, false, 0);
        h.assertTrue(echo.links().size() == 1 && echo.links().get(0).ear() == Ear.BACK
                        && echo.links().get(0).source().equals(h.absolutePos(source)),
                "An Echo Plate is synced into its back, links=" + echo.links());
        h.runAfterDelay(4, () -> {
            h.assertTrue(power(h, listener) == 15, "The Echo hears the Inverse across the gap, power=" + power(h, listener));
            wrench(h, player, wrench, listener, false, 0);
            h.assertTrue(echo.links().isEmpty(), "Using the wrench again on the synced pair unsyncs it");
            h.runAfterDelay(4, () -> {
                h.assertTrue(power(h, listener) == 0, "Unsynced, the Echo goes quiet, power=" + power(h, listener));
                wrench(h, player, wrench, source, true, 0);
                h.assertTrue(PlateLinks.held(wrench) == null, "Crouch-using the held plate again lets go of it");
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void chorusHearsTwoSyncedPlatesOnTheHalvesClicked(GameTestHelper h) {
        BlockPos west = new BlockPos(1, 2, 1), east = new BlockPos(1, 2, 5), chorusPos = new BlockPos(5, 2, 3);
        plate(h, west, LogicRegistry.INVERSE_PLATE.get());
        plate(h, east, LogicRegistry.INVERSE_PLATE.get());
        LogicPlateBlockEntity chorus = plate(h, chorusPos, LogicRegistry.CHORUS_PLATE.get());
        ItemStack wrench = new ItemStack(ModItems.TOTEM_WRENCH.get());
        ServerPlayer player = wrencher(h, wrench);
        wrench(h, player, wrench, west, true, 0);
        wrench(h, player, wrench, chorusPos, false, 0.3);    // west half: the left ear
        h.assertTrue(chorus.links().size() == 1 && chorus.links().get(0).ear() == Ear.LEFT, "West half syncs the left ear");
        h.runAfterDelay(3, () -> {
            h.assertTrue(power(h, chorusPos) == 0, "Chorus waits with only its left ear live");
            wrench(h, player, wrench, east, true, 0);
            wrench(h, player, wrench, chorusPos, false, -0.3);   // east half: the right ear
            h.assertTrue(chorus.links().size() == 2 && chorus.links().get(1).ear() == Ear.RIGHT, "East half syncs the right ear");
            h.runAfterDelay(3, () -> {
                h.assertTrue(power(h, chorusPos) == 15, "Chorus sings with both synced ears live");
                h.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void brokenSourceReadsZeroAndIsForgotten(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), listener = new BlockPos(5, 2, 5);
        plate(h, source, LogicRegistry.INVERSE_PLATE.get());
        LogicPlateBlockEntity echo = plate(h, listener, LogicRegistry.ECHO_PLATE.get());
        h.assertTrue(echo.link(h.absolutePos(source), Ear.BACK), "The link is made");
        h.runAfterDelay(3, () -> {
            h.assertTrue(power(h, listener) == 15, "The synced Echo sings");
            h.setBlock(source, Blocks.AIR);
            h.runAfterDelay(3, () -> {
                h.assertTrue(power(h, listener) == 0, "A broken source is heard as 0, power=" + power(h, listener));
                h.assertTrue(echo.links().isEmpty(), "A broken source is forgotten");
                h.succeed();
            });
        });
    }

    /** Two Inverse plates synced to hear each other: a clean one-tick clock, never a crash or a runaway. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aRingOfSyncedPlatesStaysBounded(GameTestHelper h) {
        BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(4, 2, 4), echoA = new BlockPos(1, 2, 5), echoB = new BlockPos(5, 2, 1);
        LogicPlateBlockEntity first = plate(h, a, LogicRegistry.INVERSE_PLATE.get());
        LogicPlateBlockEntity second = plate(h, b, LogicRegistry.INVERSE_PLATE.get());
        h.assertTrue(first.link(h.absolutePos(b), Ear.BACK) && second.link(h.absolutePos(a), Ear.BACK), "A two-plate ring is allowed");
        h.assertTrue(!first.link(h.absolutePos(a), Ear.BACK), "A plate cannot be synced to itself");
        h.assertTrue(!first.link(h.absolutePos(b), Ear.LEFT), "A source is heard once, not twice");
        // Two Echo plates that hear each other hold whatever they agree on: nothing, with nothing wired in.
        LogicPlateBlockEntity hold = plate(h, echoA, LogicRegistry.ECHO_PLATE.get());
        LogicPlateBlockEntity other = plate(h, echoB, LogicRegistry.ECHO_PLATE.get());
        hold.link(h.absolutePos(echoB), Ear.BACK);
        other.link(h.absolutePos(echoA), Ear.BACK);
        List<int[]> samples = new ArrayList<>();
        h.onEachTick(() -> samples.add(new int[]{power(h, a), power(h, b), power(h, echoA), power(h, echoB)}));
        h.runAfterDelay(30, () -> {
            int flips = 0;
            for (int i = 0; i < samples.size(); i++) {
                int[] s = samples.get(i);
                h.assertTrue((s[0] == 0 || s[0] == 15) && s[0] == s[1], "The ring flips in step, sample " + i + ": " + s[0] + "/" + s[1]);
                h.assertTrue(s[2] == 0 && s[3] == 0, "An Echo ring with no input stays quiet");
                if (i > 0 && samples.get(i - 1)[0] != s[0]) flips++;
            }
            h.assertTrue(flips <= samples.size(), "At most one flip a tick, flips=" + flips);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void plateLinksSurviveSaveAndLoad(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        LogicPlateBlockEntity tally = plate(h, pos, LogicRegistry.TALLY_PLATE.get());
        BlockPos one = h.absolutePos(new BlockPos(1, 2, 1)), two = h.absolutePos(new BlockPos(5, 2, 5));
        tally.link(one, Ear.BACK);
        tally.link(two, Ear.LEFT);
        var registries = h.getLevel().registryAccess();
        CompoundTag saved = tally.saveWithoutMetadata(registries);
        var copy = new LogicPlateBlockEntity(tally.getBlockPos(), tally.getBlockState());
        copy.loadWithComponents(saved, registries);
        h.assertTrue(copy.links().equals(tally.links()), "Links load back as saved: " + copy.links());
        h.assertTrue(copy.saveWithoutMetadata(registries).equals(saved), "Saving the loaded plate writes the same tag");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void plateLinksKeepToRangeAndWorld(GameTestHelper h) {
        BlockPos listener = new BlockPos(3, 2, 3);
        LogicPlateBlockEntity echo = plate(h, listener, LogicRegistry.ECHO_PLATE.get());
        LogicPlateBlockEntity heartbeat = plate(h, new BlockPos(5, 2, 5), LogicRegistry.HEARTBEAT_PLATE.get());
        plate(h, new BlockPos(1, 2, 1), LogicRegistry.INVERSE_PLATE.get());
        BlockPos far = h.absolutePos(listener).east(TribalConfig.plateLinkRange() + 8);
        ItemStack wrench = new ItemStack(ModItems.TOTEM_WRENCH.get());
        ServerPlayer player = wrencher(h, wrench);
        wrench.set(LogicRegistry.HELD_PLATE.get(), GlobalPos.of(h.getLevel().dimension(), far));
        wrench(h, player, wrench, listener, false, 0);
        h.assertTrue(echo.links().isEmpty(), "A plate beyond the reach is refused");
        wrench.set(LogicRegistry.HELD_PLATE.get(), GlobalPos.of(Level.NETHER, h.absolutePos(new BlockPos(1, 2, 1))));
        wrench(h, player, wrench, listener, false, 0);
        h.assertTrue(echo.links().isEmpty(), "A plate in another world is refused");
        wrench.set(LogicRegistry.HELD_PLATE.get(), GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(1, 2, 1))));
        wrench(h, player, wrench, new BlockPos(5, 2, 5), false, 0);
        h.assertTrue(heartbeat.links().isEmpty(), "A Heartbeat hears nothing, so it takes no link");
        // A link made before the reach shrank is kept but heard as 0, and never loads anything to find out.
        h.assertTrue(echo.link(far, Ear.BACK), "A raw link can name any position");
        h.runAfterDelay(3, () -> {
            h.assertTrue(power(h, listener) == 0, "A link beyond the reach is heard as 0");
            h.assertTrue(echo.links().size() == 1, "and kept, in case the reach is raised again");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void strangersCannotSyncHoldOrRetunePlates(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), listener = new BlockPos(5, 2, 5), theirsPos = new BlockPos(1, 2, 5),
                tallyPos = new BlockPos(5, 2, 1), loosePos = new BlockPos(3, 2, 3);
        LogicPlateBlockEntity src = plate(h, source, LogicRegistry.INVERSE_PLATE.get());
        LogicPlateBlockEntity echo = plate(h, listener, LogicRegistry.ECHO_PLATE.get());
        LogicPlateBlockEntity theirs = plate(h, theirsPos, LogicRegistry.INVERSE_PLATE.get());
        LogicPlateBlockEntity tally = plate(h, tallyPos, LogicRegistry.TALLY_PLATE.get());
        LogicPlateBlockEntity loose = plate(h, loosePos, LogicRegistry.ECHO_PLATE.get());
        ItemStack ownerWrench = new ItemStack(ModItems.TOTEM_WRENCH.get()), strangerWrench = new ItemStack(ModItems.TOTEM_WRENCH.get());
        ServerPlayer owner = wrencher(h, ownerWrench), stranger = wrencher(h, strangerWrench);
        try {
            placeBy(h, source, owner);
            placeBy(h, listener, owner);
            placeBy(h, tallyPos, owner);
            placeBy(h, theirsPos, stranger);
            placeBy(h, loosePos, null);
            h.assertTrue(owner.getUUID().equals(src.owner()) && owner.getUUID().equals(echo.owner())
                    && stranger.getUUID().equals(theirs.owner()), "Setting a plate down makes the placer its owner");
            h.assertTrue(loose.owner() == null, "A plate set down by no player belongs to no one");

            wrench(h, stranger, strangerWrench, source, true, 0);
            h.assertTrue(PlateLinks.held(strangerWrench) == null, "A stranger cannot hold another camp's plate's song");
            wrench(h, stranger, strangerWrench, theirsPos, true, 0);
            h.assertTrue(PlateLinks.held(strangerWrench) != null, "A stranger holds their own plate's song");
            h.assertTrue(wrench(h, stranger, strangerWrench, listener, false, 0) == InteractionResult.FAIL && echo.links().isEmpty(),
                    "A stranger cannot sync their plate into another camp's plate");

            wrench(h, owner, ownerWrench, source, true, 0);
            wrench(h, owner, ownerWrench, listener, false, 0);
            h.assertTrue(echo.links().size() == 1, "The owner syncs their own plates");
            wrench(h, owner, ownerWrench, theirsPos, true, 0);
            h.assertTrue(PlateLinks.held(ownerWrench) != null && PlateLinks.held(ownerWrench).pos().equals(h.absolutePos(source)),
                    "The owner cannot hold a stranger's plate either, and keeps the song already held");

            strangerWrench.set(LogicRegistry.HELD_PLATE.get(), GlobalPos.of(h.getLevel().dimension(), h.absolutePos(source)));
            wrench(h, stranger, strangerWrench, listener, false, 0);
            h.assertTrue(echo.links().size() == 1, "A stranger cannot unsync another camp's plate");
            strangerWrench.remove(LogicRegistry.HELD_PLATE.get());
            h.assertTrue(wrench(h, stranger, strangerWrench, listener, false, 0) == InteractionResult.CONSUME,
                    "Reading another camp's syncs stays open");

            int before = threshold(h, tally);
            crouchUse(h, stranger, tallyPos);
            h.assertTrue(threshold(h, tally) == before, "A stranger cannot change a Tally's count");
            crouchUse(h, owner, tallyPos);
            h.assertTrue(threshold(h, tally) != before, "The owner changes their Tally's count");

            owner.setItemInHand(InteractionHand.MAIN_HAND, ownerWrench);
            wrench(h, owner, ownerWrench, listener, false, 0);
            h.assertTrue(echo.links().isEmpty(), "The owner unsyncs their own plate");
            h.succeed();
        } finally { leave(h, owner, stranger); }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void campMatesAndUnownedPlatesAreOpen(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), listener = new BlockPos(5, 2, 5), oldSource = new BlockPos(1, 2, 5),
                oldListener = new BlockPos(5, 2, 1), oldTally = new BlockPos(3, 2, 3);
        LogicPlateBlockEntity src = plate(h, source, LogicRegistry.INVERSE_PLATE.get());
        LogicPlateBlockEntity echo = plate(h, listener, LogicRegistry.ECHO_PLATE.get());
        plate(h, oldSource, LogicRegistry.INVERSE_PLATE.get());
        LogicPlateBlockEntity oldEcho = plate(h, oldListener, LogicRegistry.ECHO_PLATE.get());
        LogicPlateBlockEntity tally = plate(h, oldTally, LogicRegistry.TALLY_PLATE.get());
        ItemStack mateWrench = new ItemStack(ModItems.TOTEM_WRENCH.get());
        ServerPlayer owner = VerificationPlayers.inLevel(h), mate = wrencher(h, mateWrench);
        var data = Camps.data(h.getLevel().getServer());
        var camp = data.create("Plate camp " + UUID.randomUUID().toString().substring(0, 4), owner.getUUID());
        try {
            h.assertTrue(camp != null && data.invite(camp, owner.getUUID(), mate.getUUID()) && data.join(camp, mate.getUUID()),
                    "The test camp forms");
            src.setOwner(owner.getUUID());
            echo.setOwner(owner.getUUID());
            wrench(h, mate, mateWrench, source, true, 0);
            wrench(h, mate, mateWrench, listener, false, 0);
            h.assertTrue(echo.links().size() == 1, "A camp mate syncs the owner's plates");

            // Plates set down before plates had owners carry no owner tag and stay open to everyone.
            data.leave(mate.getUUID());
            wrench(h, mate, mateWrench, oldSource, true, 0);
            wrench(h, mate, mateWrench, oldListener, false, 0);
            h.assertTrue(oldEcho.links().size() == 1, "Anyone syncs plates that belong to no one");
            int before = threshold(h, tally);
            crouchUse(h, mate, oldTally);
            h.assertTrue(threshold(h, tally) != before, "Anyone changes the count of a plate that belongs to no one");
            mate.setItemInHand(InteractionHand.MAIN_HAND, mateWrench);
            wrench(h, mate, mateWrench, source, true, 0);
            h.assertTrue(PlateLinks.held(mateWrench).pos().equals(h.absolutePos(oldSource)),
                    "Once out of the camp, the owner's plates are refused again");
            h.succeed();
        } finally {
            data.leave(mate.getUUID());
            data.leave(owner.getUUID());
            leave(h, owner, mate);
        }
    }

    @GameTest(template = "empty")
    public static void plateOwnerSurvivesSaveAndLoad(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        LogicPlateBlockEntity owned = plate(h, new BlockPos(1, 2, 1), LogicRegistry.MEMORY_PLATE.get());
        LogicPlateBlockEntity unowned = plate(h, new BlockPos(3, 2, 3), LogicRegistry.MEMORY_PLATE.get());
        UUID who = UUID.randomUUID();
        owned.setOwner(who);
        var copy = new LogicPlateBlockEntity(owned.getBlockPos(), owned.getBlockState());
        copy.loadWithComponents(owned.saveWithoutMetadata(registries), registries);
        h.assertTrue(who.equals(copy.owner()), "A plate's owner loads back as saved");
        CompoundTag bare = unowned.saveWithoutMetadata(registries);
        var unownedCopy = new LogicPlateBlockEntity(unowned.getBlockPos(), unowned.getBlockState());
        unownedCopy.loadWithComponents(bare, registries);
        h.assertTrue(!bare.contains("Owner") && unownedCopy.owner() == null, "A plate with no owner saves and loads none");

        BlockPos versePos = new BlockPos(5, 2, 5);
        h.setBlock(versePos.below(), Blocks.STONE);
        h.setBlock(versePos, LogicRegistry.VERSE_ANSWER.get().defaultBlockState().setValue(VerseLinkBlock.FACING, Direction.UP));
        var verse = (VerseLinkBlockEntity) h.getBlockEntity(versePos);
        verse.setOwner(who);
        var verseCopy = new VerseLinkBlockEntity(verse.getBlockPos(), verse.getBlockState());
        verseCopy.loadWithComponents(verse.saveWithoutMetadata(registries), registries);
        h.assertTrue(who.equals(verseCopy.owner()), "A Verse plate's owner loads back as saved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void strangersCannotRetuneOrTurnAVersePlate(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos, LogicRegistry.VERSE_CALL.get().defaultBlockState().setValue(VerseLinkBlock.FACING, Direction.UP));
        var verse = (VerseLinkBlockEntity) h.getBlockEntity(pos);
        ItemStack wrench = new ItemStack(ModItems.TOTEM_WRENCH.get());
        ServerPlayer owner = VerificationPlayers.inLevel(h), stranger = wrencher(h, wrench);
        try {
            owner.setGameMode(GameType.SURVIVAL);
            placeBy(h, pos, owner);
            h.assertTrue(owner.getUUID().equals(verse.owner()), "Setting a Verse plate down makes the placer its owner");
            crouchUse(h, stranger, pos);
            h.assertTrue(verse.verse() == 0, "A stranger cannot retune another camp's Verse plate");
            stranger.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            h.assertTrue(wrench(h, stranger, wrench, pos, false, 0) == InteractionResult.FAIL
                    && h.getBlockState(pos).getValue(VerseLinkBlock.FACING) == Direction.UP, "A stranger cannot turn it with the wrench");
            crouchUse(h, owner, pos);
            h.assertTrue(verse.verse() == 1, "The owner retunes their Verse plate");
            h.succeed();
        } finally { leave(h, owner, stranger); }
    }
}
